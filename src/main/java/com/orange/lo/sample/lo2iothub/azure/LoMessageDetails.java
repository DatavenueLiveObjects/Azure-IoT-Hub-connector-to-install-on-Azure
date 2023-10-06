package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.UUID;

public class LoMessageDetails {

    private IoTHubClient ioTHubClient;
    private String message;
    private String messageId;
    private String messageCreated;
    private String deviceId;


    public LoMessageDetails(String message, IoTHubClient ioTHubClient) {
        this.ioTHubClient = ioTHubClient;
        this.message = message;
        this.messageId = UUID.randomUUID().toString();
        this.messageCreated = extractFromMessage("created");
        this.deviceId = extractFromMessage("streamId");
    }

    public void setMessage(String message) {

    }

    public IoTHubClient getIoTHubClient() {
        return ioTHubClient;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageCreated() {
        return messageCreated;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    private String extractFromMessage(String field) {
        try {
            return new JSONObject(message).getString(field);
        } catch (JSONException e) {
            return "";
        }
    }
}