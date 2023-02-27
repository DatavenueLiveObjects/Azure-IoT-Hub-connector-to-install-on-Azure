package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClientOptions;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    
    private List<MultiplexingClientExt> multiplexingClientExtList;
    private Map<String, DeviceClientExt> deviceClientMap;
    private LoCommandSender loCommandSender;
    private String host;
    private String connectorType;
    private String connectorVersion;
    
    public DeviceClientManager(String host, String connectorType, String connectorVersion) throws IotHubClientException {
        this.host = host;
        this.connectorType = connectorType;
        this.connectorVersion = connectorVersion;
        this.deviceClientMap = new HashMap<String, DeviceClientExt>();
        this.multiplexingClientExtList = new LinkedList<>();
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public DeviceClient createDeviceClient(Device device) throws InterruptedException, IotHubClientException, TimeoutException {
        
        DeviceClient deviceClient = createNewDeviceClient(device);
        
        MultiplexingClientExt freeMultiplexingClientManager = null;
        for (MultiplexingClientExt multiplexingClientManager : multiplexingClientExtList) {
            if (multiplexingClientManager.getClient().getRegisteredDeviceCount() < MultiplexingClient.MAX_MULTIPLEX_DEVICE_COUNT_AMQPS) {
                freeMultiplexingClientManager = multiplexingClientManager;
                break;
            }
        }
        if (freeMultiplexingClientManager == null) {
            freeMultiplexingClientManager = createMultiplexingClientManager();
            multiplexingClientExtList.add(freeMultiplexingClientManager);
        }
        freeMultiplexingClientManager.registerDeviceClient(deviceClient);
        deviceClientMap.put(deviceClient.getConfig().getDeviceId(),
                new DeviceClientExt(deviceClient, freeMultiplexingClientManager));
        
        return deviceClient;
    }
    
    private DeviceClient createNewDeviceClient(Device device) {
        String connString = getConnectionString(device);
        DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
        deviceClient.setMessageCallback(new MessageCallbackMqtt(), device.getDeviceId());
        return deviceClient;
    }
    
    public DeviceClient getDeviceClient(String deviceClientId) {
        return deviceClientMap.get(deviceClientId).getClient();
    }

    public boolean containsDeviceClient(String deviceClientId) {
        return deviceClientMap.containsKey(deviceClientId);
    }

    public void removeDeviceClient(String deviceClientId) throws InterruptedException, IotHubClientException, TimeoutException {
        for (MultiplexingClientExt multiplexingClientManager : multiplexingClientExtList) {
            if (multiplexingClientManager.getClient().isDeviceRegistered(deviceClientId)) {
                multiplexingClientManager.unregisterDeviceClient(deviceClientMap.get(deviceClientId).getClient());
                deviceClientMap.remove(deviceClientId);
            }
        }
    }

    private MultiplexingClientExt createMultiplexingClientManager() throws IotHubClientException {
        LOG.debug("Creating MultiplexingClientManager ...");
        MultiplexingClientOptions options = MultiplexingClientOptions.builder().build();
        final MultiplexingClient multiplexingClient = new MultiplexingClient(host, IotHubClientProtocol.AMQPS, options);
        MultiplexingClientExt multiplexingClientManager = new MultiplexingClientExt(multiplexingClient, connectorType + "-" + connectorVersion + "-" +UUID.randomUUID());
        multiplexingClientManager.open();
        LOG.debug("MultiplexingClientManager created");
        return multiplexingClientManager;
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
