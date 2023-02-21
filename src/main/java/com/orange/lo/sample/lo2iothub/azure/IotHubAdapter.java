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
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClientOptions;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.CommandException;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

public class IotHubAdapter {

    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IoTDeviceProvider ioTDeviceProvider;
    private LoCommandSender loCommandSender;
    private MessageSender messageSender;
    private IotClientCache iotClientCache;
    private AzureIotHubProperties iotHubProperties;
    private ExecutorService executorService;
    private boolean deviceSynchronization;
    private MultiplexingClientManager multiplexingClientManager;
    private Map<String, DeviceClientManager> deviceManagers = new HashMap<>();

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, MessageSender messageSender,
            IotClientCache iotClientCache, AzureIotHubProperties iotHubProperties, boolean deviceSynchronization) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.messageSender = messageSender;
        this.iotClientCache = iotClientCache;
        this.iotHubProperties = iotHubProperties;
        this.deviceSynchronization = deviceSynchronization;
        this.executorService = Executors.newCachedThreadPool();
        init();
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }
    
    
    
    private void init() {
        MultiplexingClientOptions options = MultiplexingClientOptions.builder().build();
        final MultiplexingClient multiplexingClient = new MultiplexingClient(iotHubProperties.getIotHostName(), IotHubClientProtocol.AMQPS, options);
        multiplexingClientManager = new MultiplexingClientManager(multiplexingClient, "MultiplexingClient");
        
        System.out.println("Opening multiplexed connection ");
        try
        {
            multiplexingClientManager.open();
        }
        catch (IotHubClientException e)
        {
            // error is logged by the MultiplexingClientManager, no need to log it here, too
            System.out.println("Exiting sample...");
            System.exit(-1);
        }
        System.out.println("Multiplexed connection opened successfully");
    }

    public void sendMessage(String msg) {
        try {
            String loClientId = getSourceDeviceId(msg);
            Message message = new Message(msg);
            
            DeviceClient deviceClient = deviceManagers.get(loClientId).getClient();
            if (deviceClient == null) {
                deviceClient = createDeviceClient(loClientId);
                //deviceClient.sendEventAsync(message, new TelemetryAcknowledgedEventCallback(), message);
                //messageSender.sendMessage(msg, deviceClient);
//            } else {
//                deviceClient.sendEventAsync(message, new TelemetryAcknowledgedEventCallback(), message);
////                executorService.execute(() -> {
////                    messageSender.sendMessage(msg, createdDeviceClient);
////                });
            }
            deviceClient.sendEventAsync(message, new TelemetryAcknowledgedEventCallback(), message);
        } catch (IllegalArgumentException e) {
            LOG.error("Error while sending message", e);
        } catch (JSONException e) {
            LOG.error("Cannot retrieve device id from message, message not sent {}", msg);
        } catch (Exception e) {
            LOG.error("Cannot send message", e);
        }
    }

    private static String getSourceDeviceId(String msg) throws JSONException {
        return new JSONObject(msg).getJSONObject("metadata").getString("source");
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                multiplexingClientManager.unregisterDeviceClient(deviceManagers.get(deviceId).getClient());
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                System.exit(-1);
            }
            ioTDeviceProvider.deleteDevice(deviceId);
        } else {
            throw new DeviceSynchronizationException(deviceId);
        }
    }

    public DeviceClient createDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {
            System.out.println("Creating device client that will be multiplexed " + deviceId);
            Device device = ioTDeviceProvider.getDevice(deviceId);
            // no device in iot hub
            if (device == null) {
                if (deviceSynchronization) {
                    device = ioTDeviceProvider.createDevice(deviceId);
                } else {
                    throw new DeviceSynchronizationException(deviceId);
                }
                // device exists but device client doesn't
            }

            String connString = getConnectionString(device);
            DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.AMQPS);
            deviceClient.setMessageCallback(new MessageCallbackMqtt(), deviceId);
            deviceManagers.put(deviceId, new DeviceClientManager(deviceClient, multiplexingClientManager));

            System.out.println("Registering " + deviceId + " client to the multiplexing client...");
            try {
                multiplexingClientManager.registerDeviceClient(deviceClient);
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                // error is logged by the MultiplexingClientManager, no need to log it here, too
                System.out.println("Exiting sample...");
                System.exit(-1);
            }
            System.out.println(
                    "*********************** " + multiplexingClientManager.getClient().getRegisteredDeviceCount());
            return deviceClient;
        }
    }

//    private DeviceClient createDeviceClient(Device device) {
//        String deviceId = device.getDeviceId();
//        if (iotClientCache.get(deviceId) == null) {
//            try {
//                String connString = getConnectionString(device);
//                DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.MQTT);
//                deviceClient.setMessageCallback(new MessageCallbackMqtt(), deviceId);
//                deviceClient.setOperationTimeout(iotHubProperties.getDeviceClientConnectionTimeout());
//                deviceClient.setRetryPolicy((currentRetryCount, lastException) -> new RetryDecision(false, 0));
//                deviceClient.setConnectionStatusChangeCallback(new ConnectionStatusChangeCallback(), deviceId);
//                deviceClient.open(false);
//                iotClientCache.add(deviceId, deviceClient);
//                if (LOG.isInfoEnabled()) {
//                    String cleanDeviceId = StringEscapeUtils.escapeHtml4(deviceId);
//                    LOG.info("Device client created for {}", cleanDeviceId);
//                }
//                return deviceClient;
//            } catch (IotHubClientException e) {
//                String cleanDeviceId = StringEscapeUtils.escapeHtml4(deviceId);
//                LOG.error("Error while creating device client for " + cleanDeviceId, e);
//                return null;
//            }
//        }
//        return iotClientCache.get(deviceId);
//    }

    private String getConnectionString(Device device) {
        String iotHostName = iotHubProperties.getIotHostName();
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, iotHostName, deviceId, primaryKey);
    }

//    protected class ConnectionStatusChangeCallback implements IotHubConnectionStatusChangeCallback {
//
//        @Override
//        public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
//            if (IotHubConnectionStatus.DISCONNECTED == connectionStatusChangeContext.getNewStatus()) {
//                String deviceId = connectionStatusChangeContext.getCallbackContext().toString();
//                LOG.debug("Device client disconnected for {}, trying to recreate ...", deviceId);
//                iotClientCache.remove(deviceId);
//                try {
//                    createDeviceClient(deviceId);
//                } catch (Exception e) {
//                    LOG.error("Cannot create device client", e);
//                }
//            }
//
//        }
//    }

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
            try {
                loCommandSender.send(deviceId, new String(message.getBytes()));
                return IotHubMessageResult.COMPLETE;
            } catch (CommandException e) {
                LOG.error("Cannot send command", e);
                return IotHubMessageResult.REJECT;
            }
        }
    }

    public List<IoTDevice> getDevices() {
        return ioTDeviceProvider.getDevices();
    }
    private static class TelemetryAcknowledgedEventCallback implements MessageSentCallback
    {
        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context)
        {
            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
            System.out.println(
                    "IoT Hub responded to message " + sentMessage.getMessageId() + " with status " + status.name());
        }
    }
}