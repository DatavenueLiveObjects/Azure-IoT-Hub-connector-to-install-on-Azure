/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.RetryDecision;
import com.microsoft.azure.sdk.iot.service.Device;
import com.orange.lo.sample.lo2iothub.exceptions.CommandException;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.text.StringEscapeUtils;
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

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, LoCommandSender loCommandSender,
                         MessageSender messageSender, IotClientCache iotClientCache,
                         AzureIotHubProperties iotHubProperties) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.loCommandSender = loCommandSender;
        this.messageSender = messageSender;
        this.iotClientCache = iotClientCache;
        this.iotHubProperties = iotHubProperties;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void sendMessage(org.springframework.messaging.Message<String> msg) {
        try {
            String loClientId = getSourceDeviceId(msg.getPayload());
            DeviceClient deviceClient = iotClientCache.get(loClientId);
            if (deviceClient != null) {
                messageSender.sendMessage(msg, deviceClient);
            } else {
                executorService.execute(() -> {
                    DeviceClient createdDeviceClient = createDeviceClient(loClientId);
                    messageSender.sendMessage(msg, createdDeviceClient);
                });
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Error while sending message", e);
        } catch (JSONException e) {
            LOG.error("Cannot retrieve device id from message, message not sent {}", msg.getPayload());
        } catch (Exception e) {
            LOG.error("Cannot send message", e);
        }
    }

    private static String getSourceDeviceId(String msg) throws JSONException {
        return new JSONObject(msg).getJSONObject("metadata").getString("source");
    }

    public void deleteDevice(String deviceId) {
        iotClientCache.remove(deviceId);
        ioTDeviceProvider.deleteDevice(deviceId);
    }

    public DeviceClient createDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {
            Device device = ioTDeviceProvider.getDevice(deviceId);
            // no device in iot hub
            if (device == null) {
                device = ioTDeviceProvider.createDevice(deviceId);
                DeviceClient deviceClient = iotClientCache.get(deviceId);
                // make sure that if device client exists we have to close it
                if (deviceClient != null) {
                    iotClientCache.remove(deviceId);
                }
                return createDeviceClient(device);
                // device exists but device client doesn't
            } else if (iotClientCache.get(deviceId) == null) {
                return createDeviceClient(device);
            } else {
                return iotClientCache.get(deviceId);
            }
        }
    }

    private DeviceClient createDeviceClient(Device device) {
        String deviceId = device.getDeviceId();
        if (iotClientCache.get(deviceId) == null) {
            try {
                String connString = getConnectionString(device);
                DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.MQTT);
                deviceClient.setMessageCallback(new MessageCallbackMqtt(), deviceId);
                deviceClient.setOperationTimeout(iotHubProperties.getDeviceClientConnectionTimeout());
                deviceClient.setRetryPolicy((currentRetryCount, lastException) -> new RetryDecision(false, 0));
                deviceClient.registerConnectionStatusChangeCallback(new ConnectionStatusChangeCallback(), deviceId);
                deviceClient.open();
                iotClientCache.add(deviceId, deviceClient);
                LOG.info("Device client created for {}", deviceId);
                return deviceClient;
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error while creating device client", e);
                return null;
            }
        }
        return iotClientCache.get(deviceId);
    }

    private String getConnectionString(Device device) {
        String iotHostName = iotHubProperties.getIotHostName();
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, iotHostName, deviceId, primaryKey);
    }

    protected class ConnectionStatusChangeCallback implements IotHubConnectionStatusChangeCallback {

        @Override
        public void execute(IotHubConnectionStatus status, IotHubConnectionStatusChangeReason statusChangeReason,
                            Throwable throwable, Object callbackContext) {

            if (IotHubConnectionStatus.DISCONNECTED == status) {
                String deviceId = callbackContext.toString();
                LOG.debug("Device client disconnected for {}, trying to recreate ...", deviceId);
                iotClientCache.remove(deviceId);
                try {
                    createDeviceClient(deviceId);
                } catch (Exception e) {
                    LOG.error("Cannot create device client", e);
                }
            }
        }
    }

    protected class MessageCallbackMqtt implements MessageCallback {

        @Override
        public IotHubMessageResult execute(Message msg, Object context) {
            String deviceId = context.toString();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received command for device: {} with content {}", deviceId, new String(msg.getBytes(),
                        Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
                for (MessageProperty messageProperty : msg.getProperties()) {
                    LOG.debug("{} : {}", messageProperty.getName(), messageProperty.getValue());
                }
            }
            try {
                loCommandSender.send(deviceId, new String(msg.getBytes()));
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
}
