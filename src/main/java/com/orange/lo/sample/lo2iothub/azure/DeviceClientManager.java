package com.orange.lo.sample.lo2iothub.azure;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;

public class DeviceClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    private AtomicInteger multiplexingClientIndex;
    private List<MultiplexingClient> multiplexingClientList;
    private Map<String, IoTHubClient> ioTHubClientMap;
    private LoCommandSender loCommandSender;

    private ScheduledExecutorService registerTaskScheduler;
    private Phaser phaser = new Phaser(1);
    private List<DeviceClient> deviceClientsToRegister = new ArrayList<DeviceClient>();
    private final Object operationLock = new Object();

    private String host;

    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;


    public DeviceClientManager(String host, int period, ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint) throws IotHubClientException {
        this.host = host;
        this.ioTHubClientMap = Collections.synchronizedMap(new HashMap<String, IoTHubClient>());
        this.multiplexingClientList = Collections.synchronizedList(new LinkedList<>());
        this.multiplexingClientIndex = new AtomicInteger();

        this.registerTaskScheduler = Executors.newScheduledThreadPool(1);
        this.registerTaskScheduler.scheduleAtFixedRate(() -> registerDeviceClientsAsMultiplexed(), 0, period, TimeUnit.MILLISECONDS);
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    private void registerDeviceClientsAsMultiplexed() {
        synchronized (this.operationLock) {
            try {
                while (deviceClientsToRegister.size() > 0) {
                    MultiplexingClient multiplexingClient = getFreeMultiplexingClient();
                    int registeredDeviceCount = multiplexingClient.getRegisteredDeviceCount();
                    int remainingDeviceCount = MultiplexingClient.MAX_MULTIPLEX_DEVICE_COUNT_AMQPS - registeredDeviceCount;
                    int subListSize = Math.min(remainingDeviceCount, deviceClientsToRegister.size());
                    List<DeviceClient> subList = deviceClientsToRegister.subList(0, subListSize);
                    multiplexingClient.registerDeviceClients(subList);
                    subList.forEach(dc -> ioTHubClientMap.put(dc.getConfig().getDeviceId(), new IoTHubClient(dc, multiplexingClient)));
                    subList.clear();
                    LOG.info("Registered {} clients", subListSize);
                }
            } catch (IotHubClientException | InterruptedException e) {
                LOG.error("Cannot register clients", e);
            } finally {
                phaser.arrive();
            }
        }
    }

    private MultiplexingClient getFreeMultiplexingClient() throws IotHubClientException {
        MultiplexingClient freeMultiplexingClient = null;
        for (MultiplexingClient multiplexingClient : multiplexingClientList) {
            if (multiplexingClient.getRegisteredDeviceCount() < MultiplexingClient.MAX_MULTIPLEX_DEVICE_COUNT_AMQPS) {
                freeMultiplexingClient = multiplexingClient;
                break;
            }
        }
        if (freeMultiplexingClient == null) {
            freeMultiplexingClient = createMultiplexingClientManager();
            multiplexingClientList.add(freeMultiplexingClient);
        }
        return freeMultiplexingClient;
    }

    public void createDeviceClient(Device device) {
        DeviceClient deviceClient = createNewDeviceClient(device);
        synchronized (this.operationLock) {
            phaser.register();
            deviceClientsToRegister.add(deviceClient);
        }
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    private DeviceClient createNewDeviceClient(Device device) {
        String connString = getConnectionString(device);
        DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
        deviceClient.setMessageCallback(new MessageCallbackMqtt(), device.getDeviceId());
        return deviceClient;
    }

    public IoTHubClient getDeviceClient(String deviceClientId) {
        return ioTHubClientMap.get(deviceClientId);
    }

    public boolean containsDeviceClient(String deviceClientId) {
        return ioTHubClientMap.containsKey(deviceClientId);
    }

    public void removeDeviceClient(String deviceClientId) throws InterruptedException, IotHubClientException, TimeoutException {
        for (MultiplexingClient multiplexingClient : multiplexingClientList) {
            if (multiplexingClient.isDeviceRegistered(deviceClientId)) {
                multiplexingClient.unregisterDeviceClient(ioTHubClientMap.get(deviceClientId).getDeviceClient());
                ioTHubClientMap.remove(deviceClientId);
            }
        }
    }

    private MultiplexingClient createMultiplexingClientManager() throws IotHubClientException {
        final int clientNo = multiplexingClientIndex.incrementAndGet();
        LOG.info("Creating MultiplexingClient nr {}", clientNo);

        final MultiplexingClient multiplexingClient = new MultiplexingClient(host, IotHubClientProtocol.AMQPS, null);
        multiplexingClient.setConnectionStatusChangeCallback(
                new IotHubConnectionStatusChangeCallbackImpl(connectorHealthActuatorEndpoint, multiplexingClient, clientNo), multiplexingClient);

        connect(multiplexingClient, clientNo);
        LOG.info("MultiplexingClient nr {} created", clientNo);
        return multiplexingClient;
    }

    private void connect(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Opening MultiplexingClient nr {}", clientNo);

        Failsafe.with(getRetryPolicy(clientNo)).run(() -> {
            multiplexingClient.open(true);
            LOG.info("Opening MultiplexingClient nr {} success", clientNo);
        });
    }

    private void reconnectMultiplexingClient(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Reconnecting MultiplexingClient nr {} " + clientNo);

        try {
            LOG.info("Closing MultiplexingClient nr {} " + clientNo);
            multiplexingClient.close();
            connect(multiplexingClient, clientNo);
            LOG.info("Closing MultiplexingClient nr {} success" + clientNo);
        } catch (Exception ex) {
            LOG.error("Reconnecting MultiplexingClient nr {} error because of {}", clientNo, ex.getMessage());
        }
    }

    private RetryPolicy<Void> getRetryPolicy(int clientNo) {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
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