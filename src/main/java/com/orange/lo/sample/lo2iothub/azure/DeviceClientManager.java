package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.ConnectionStatusChangeContext;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class DeviceClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    private AtomicInteger multiplexingClientNo;
    private List<MultiplexingClient> multiplexingClientList;
    private Map<String, DeviceClient> deviceClientMap;
    private LoCommandSender loCommandSender;
    private String host;
    
    private final Object operationLock = new Object();
    
    public DeviceClientManager(String host) throws IotHubClientException {
        this.host = host;
        this.deviceClientMap = Collections.synchronizedMap(new HashMap<String, DeviceClient>());
        this.multiplexingClientList = Collections.synchronizedList(new LinkedList<>());
        this.multiplexingClientNo = new AtomicInteger();
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public DeviceClient createDeviceClient(Device device) throws InterruptedException, IotHubClientException, TimeoutException {
        DeviceClient deviceClient = createNewDeviceClient(device);

        List<DeviceClient> clientList = new ArrayList<>();
        clientList.add(deviceClient);
        this.registerDeviceClients(clientList);

        return deviceClient;
    }

    public List<DeviceClient> createDeviceClients(Collection<Device> devices) throws InterruptedException,
            IotHubClientException, TimeoutException {
        List<DeviceClient> deviceClients = devices.stream()
                .map(this::createNewDeviceClient)
                .collect(Collectors.toList());
        this.registerDeviceClients(deviceClients);

        return deviceClients;
    }

    private void registerDeviceClients(Collection<DeviceClient> deviceClients) throws IotHubClientException, InterruptedException {
        MultiplexingClient freeMultiplexingClient = null;
        synchronized (this.operationLock) {
            for (MultiplexingClient multiplexingClient : multiplexingClientList) {
                int registeredDeviceCount = multiplexingClient.getRegisteredDeviceCount();
                if (registeredDeviceCount + deviceClients.size() < MultiplexingClient.MAX_MULTIPLEX_DEVICE_COUNT_AMQPS) {
                    freeMultiplexingClient = multiplexingClient;
                    break;
                }
            }
            if (freeMultiplexingClient == null) {
                freeMultiplexingClient = createMultiplexingClientManager();
                multiplexingClientList.add(freeMultiplexingClient);
            }
        }

        freeMultiplexingClient.registerDeviceClients(deviceClients);
        Map<String, DeviceClient> collect = deviceClients.stream()
                .collect(Collectors.toMap(DeviceClientManager::getDeviceId, deviceClient -> deviceClient));
        deviceClientMap.putAll(collect);
    }

    private static String getDeviceId(DeviceClient deviceClient) {
        return deviceClient.getConfig().getDeviceId();
    }

    private DeviceClient createNewDeviceClient(Device device) {
        String connString = getConnectionString(device);
        DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
        deviceClient.setMessageCallback(new MessageCallbackMqtt(), device.getDeviceId());
        return deviceClient;
    }
    
    public DeviceClient getDeviceClient(String deviceClientId) {
        return deviceClientMap.get(deviceClientId);
    }

    public boolean containsDeviceClient(String deviceClientId) {
        return deviceClientMap.containsKey(deviceClientId);
    }

    public void removeDeviceClient(String deviceClientId) throws InterruptedException, IotHubClientException, TimeoutException {
        for (MultiplexingClient multiplexingClient : multiplexingClientList) {
            if (multiplexingClient.isDeviceRegistered(deviceClientId)) {
                multiplexingClient.unregisterDeviceClient(deviceClientMap.get(deviceClientId));
                deviceClientMap.remove(deviceClientId);
            }
        }
    }

    private MultiplexingClient createMultiplexingClientManager() throws IotHubClientException {
        final int clientNo = multiplexingClientNo.incrementAndGet();
        LOG.info("Creating MultiplexingClient nr {}", clientNo);

        final MultiplexingClient multiplexingClient = new MultiplexingClient(host, IotHubClientProtocol.AMQPS, null);
        multiplexingClient.setConnectionStatusChangeCallback(new IotHubConnectionStatusChangeCallback() {
            @Override
            public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
                IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
                IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
                MultiplexingClient multiplexingClient = (MultiplexingClient) connectionStatusChangeContext.getCallbackContext();
                
                Throwable throwable = connectionStatusChangeContext.getCause();

                if (throwable == null) {
                    LOG.info("Connection status changed for client: {}, status: {}, reason: {}", clientNo, status, statusChangeReason);
                } else {
                    LOG.info("Connection status changed for client: {}, status: {}, reason: {}, error: {}", clientNo, status, statusChangeReason, throwable.getMessage());
                }

                if (status != IotHubConnectionStatus.CONNECTED) {
                    reconnect(multiplexingClient, clientNo);
                }
            }
        }, multiplexingClient);

        connect(multiplexingClient, clientNo);
        LOG.info("MultiplexingClient nr {} created", clientNo);
        return multiplexingClient;
    }
    
    private void connect(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Opening MultiplexingClient nr {}", clientNo);
        
        Failsafe.with(getRetryPolicy(clientNo)).run(() -> {
            multiplexingClient.open(false);
            LOG.info("Opening MultiplexingClient nr {} success", clientNo);
        });
    }
    
    private void reconnect(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Reconnecting MultiplexingClient nr {} " + clientNo);
        
        try {
            LOG.info("Closing MultiplexingClient nr {} " + clientNo);
            multiplexingClient.close();
            LOG.info("Closing MultiplexingClient nr {} success" + clientNo);
        } catch (Exception ex) {
            LOG.error("Closing MultiplexingClient nr {} error because of {}", clientNo, ex.getMessage());
        } 
        connect(multiplexingClient, clientNo);    
    }

    private RetryPolicy<Void> getRetryPolicy(int clientNo) {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1,60, ChronoUnit.SECONDS)
                .onRetry(e -> {
                   LOG.info("Opening MultiplexingClient nr {} error because of {}, retrying ...", clientNo, e.getLastFailure().getMessage());
                });
    }
    private String getConnectionString(Device device) {
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, host, deviceId, primaryKey);
    }
    
    protected class MessageCallbackMqtt implements MessageCallback {
        @Override
        public IotHubMessageResult onCloudToDeviceMessageReceived(Message message, Object context) {
            String deviceId = context.toString();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received command for device: {} with content {}", deviceId,
                        new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
                for (MessageProperty messageProperty : message.getProperties()) {
                    LOG.debug("{} : {}", messageProperty.getName(), messageProperty.getValue());
                }
            }
            return loCommandSender.send(deviceId, new String(message.getBytes()));
        }
    }
}