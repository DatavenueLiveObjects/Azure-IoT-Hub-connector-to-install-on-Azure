package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class DeviceClientManager implements MessageCallback {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    private LoCommandSender loCommandSender;

    DeviceClientManager(Device device, String host) {
        String connString = getConnectionString(device, host);
        DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
        deviceClient.setMessageCallback(this, device);
    }

    @Override
    public IotHubMessageResult onCloudToDeviceMessageReceived(Message message, Object callbackContext) {
        DeviceClient deviceClient = (DeviceClient) callbackContext;

        logger.debug("Received command for device: {} with content {}", deviceClient.getConfig().getDeviceId(),
                new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
        for (MessageProperty messageProperty : message.getProperties()) {
            logger.debug("{} : {}", messageProperty.getName(), messageProperty.getValue());
        }
        return loCommandSender.send(deviceClient.getConfig().getDeviceId(), new String(message.getBytes()));
    }

    private String getConnectionString(Device device, String host) {
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, host, deviceId, primaryKey);
    }
}
