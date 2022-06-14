/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import com.orange.lo.sdk.fifomqtt.DataManagementFifoCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class MessageHandler implements DataManagementFifoCallback {
    private static final String DEVICE_ID_FIELD = "deviceId";
    private static final String TYPE_FIELD = "type";
    private static final String UNKNOWN_MESSAGE_TYPE = "unknown";
    private static final String DEVICE_DELETED_MESSAGE_TYPE = "deviceDeleted";
    private static final String DEVICE_CREATED_MESSAGE_TYPE = "deviceCreated";
    private static final String DATA_MESSAGE_TYPE = "dataMessage";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IotHubAdapter iotHubAdapter;
    private final Counters counterProvider;

    public MessageHandler(IotHubAdapter iotHubAdapter, Counters counterProvider) {
        this.iotHubAdapter = iotHubAdapter;
        this.counterProvider = counterProvider;
    }

    public void onMessage(String message) {
        String messageType = getMessageType(message);
        LOG.info("Received message of the type: {}", messageType);
        switch (messageType) {
            case DATA_MESSAGE_TYPE:
                handleDataMessage(message);
                break;
            case DEVICE_CREATED_MESSAGE_TYPE:
                handleDeviceCreationEvent(message);
                break;
            case DEVICE_DELETED_MESSAGE_TYPE:
                handleDeviceRemovalEvent(message);
                break;
            default:
                LOG.error("Unknown message type of message: {}", message);
        }
    }

    private void handleDataMessage(String message) {
        counterProvider.getMesasageReadCounter().increment();
        iotHubAdapter.sendMessage(message);
    }

    private void handleDeviceCreationEvent(String message) {
        Optional<String> deviceId = getDeviceId(message);
        deviceId.ifPresent(iotHubAdapter::createDeviceClient);
    }

    private void handleDeviceRemovalEvent(String message) {
        Optional<String> deviceId = getDeviceId(message);
        deviceId.ifPresent(iotHubAdapter::deleteDevice);
    }

    private static Optional<String> getDeviceId(String msg) {
        String id = null;
        try {
            id = new JSONObject(msg).getString(DEVICE_ID_FIELD);
        } catch (JSONException e) {
            LOG.error("No device id in payload");
        }
        return Optional.ofNullable(id);
    }

    private static String getMessageType(String msg) {
        try {
            return new JSONObject(msg).getString(TYPE_FIELD);
        } catch (JSONException e) {
            LOG.error("No message type in payload");
            return UNKNOWN_MESSAGE_TYPE;
        }
    }
}
