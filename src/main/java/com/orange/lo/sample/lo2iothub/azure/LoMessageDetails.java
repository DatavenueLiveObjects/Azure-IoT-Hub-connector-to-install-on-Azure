package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.UUID;

public class LoMessageDetails {
    private final DeviceClient deviceClient;
    private final String message;
    private final String messageId;
    private final String messageCreated;
    private final String deviceId;

    public LoMessageDetails(String message, DeviceClient deviceClient) {
        this.message = message;
        this.deviceClient = deviceClient;
        this.messageId = UUID.randomUUID().toString();
        this.messageCreated = extractFromMessage("created");
        this.deviceId = extractFromMessage("streamId");
    }

    public DeviceClient getDeviceClient() {
        return deviceClient;
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