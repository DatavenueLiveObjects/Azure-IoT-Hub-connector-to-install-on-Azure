/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotHubAdapter {

    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IoTDeviceProvider ioTDeviceProvider;
    private LoCommandSender loCommandSender;
    private MessageSender messageSender;
    private AzureIotHubProperties iotHubProperties;
    private boolean deviceSynchronization;
    private MultiplexingClientManager multiplexingClientManager;
    private Map<String, DeviceClientManager> deviceManagers;

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, MessageSender messageSender,
            AzureIotHubProperties iotHubProperties, MultiplexingClientManager multiplexingClientManager,
            Map<String, DeviceClientManager> deviceManagers,
            boolean deviceSynchronization) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.messageSender = messageSender;
        this.iotHubProperties = iotHubProperties;
        this.multiplexingClientManager = multiplexingClientManager;
        this.deviceManagers = deviceManagers;
        this.deviceSynchronization = deviceSynchronization;
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public void sendMessage(String loClientId, String message) {
        try {
            DeviceClient deviceClient = null;
            if (deviceManagers.get(loClientId) == null) {
                deviceClient = createDeviceClient(loClientId);
            } else {
                deviceClient = deviceManagers.get(loClientId).getClient();
            }
            messageSender.sendMessage(message, deviceClient);
        } catch (Exception e) {
            LOG.error("Cannot send message", e);
        }
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                DeviceClientManager deviceClientManager = deviceManagers.get(deviceId);
                DeviceClient client = deviceClientManager.getClient();
                // multiplexingClientManager.unregisterDeviceClient(deviceManagers.get(deviceId).getClient());
                multiplexingClientManager.unregisterDeviceClient(client);
                deviceManagers.remove(deviceId);
                ioTDeviceProvider.deleteDevice(deviceId);
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                LOG.error("Cannot delete device " + deviceId, e);
            }
        } else {
            throw new DeviceSynchronizationException(deviceId);
        }
    }

    public DeviceClient createDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {

            if (deviceManagers.get(deviceId) == null) {
                LOG.debug("Creating device client that will be multiplexed: {} ", deviceId);
                Device device = ioTDeviceProvider.getDevice(deviceId);
                // no device in iot hub
                if (device == null) {
                    if (deviceSynchronization) {
                        device = ioTDeviceProvider.createDevice(deviceId);
                    } else {
                        throw new DeviceSynchronizationException(deviceId);
                    }
                }
                String connString = getConnectionString(device);
                DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
                deviceClient.setMessageCallback(new MessageCallbackMqtt(), deviceId);
                try {
                    multiplexingClientManager.registerDeviceClient(deviceClient);
                    deviceManagers.put(deviceId, new DeviceClientManager(deviceClient, multiplexingClientManager));
                    LOG.info("Device client created for {}", deviceId);
                } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                    // error is logged by the MultiplexingClientManager, no need to log it here, too
                }
                return deviceClient;
            } else {
                return deviceManagers.get(deviceId).getClient();
            }
        }
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

    public List<IoTDevice> getDevices() {
        return ioTDeviceProvider.getDevices();
    }

    private String getConnectionString(Device device) {
        String iotHostName = iotHubProperties.getIotHostName();
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, iotHostName, deviceId, primaryKey);
    }
}