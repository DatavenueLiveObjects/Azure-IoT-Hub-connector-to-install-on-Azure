/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import com.orange.lo.sdk.fifomqtt.DataManagementFifoCallback;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

public class MessageHandler implements DataManagementFifoCallback {
    private static final String DEVICE_ID_FIELD = "deviceId";
    private static final String TYPE_FIELD = "type";
    private static final String UNKNOWN_MESSAGE_TYPE = "unknown";
    private static final String DEVICE_DELETED_MESSAGE_TYPE = "deviceDeleted";
    private static final String DEVICE_CREATED_MESSAGE_TYPE = "deviceCreated";
    private static final String DATA_MESSAGE_TYPE = "dataMessage";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IotHubAdapter iotHubAdapter;
    private final LoAdapter loAdapter;
    private final Counters counterProvider;

    public MessageHandler(IotHubAdapter iotHubAdapter, LoAdapter loAdapter, Counters counterProvider) {
        this.iotHubAdapter = iotHubAdapter;
        this.loAdapter = loAdapter;
        this.counterProvider = counterProvider;
    }

    @Override
    public void onMessage(int loMessageId, String message) {
        String messageType = getMessageType(message);
        LOG.info("Received message of the type: {}", messageType);
        switch (messageType) {
            case DATA_MESSAGE_TYPE:
                handleDataMessage(loMessageId, message);
                break;
            case DEVICE_CREATED_MESSAGE_TYPE:
                handleDeviceCreationEvent(loMessageId, message);
                break;
            case DEVICE_DELETED_MESSAGE_TYPE:
                handleDeviceRemovalEvent(loMessageId, message);
                break;
            default:
                LOG.error("Unknown message type of message: {}", message);
        }
    }

    private void handleDataMessage(int loMessageId, String message) {
        counterProvider.getMesasageReadCounter().increment();
        try {
            String sourceDeviceId = getSourceDeviceId(message);
            iotHubAdapter.sendMessage(sourceDeviceId, loMessageId, message);
        } catch (JSONException e) {
            LOG.error("Cannot read source device id from message", e);
            counterProvider.getMesasageSentFailedCounter().increment();
            loAdapter.sendMessageAck(loMessageId);
        } catch (DeviceSynchronizationException e) {
            LOG.error("Cannot send message to IoT Hub because device doesn't exist", e);
            counterProvider.getMesasageSentFailedCounter().increment();
            loAdapter.sendMessageAck(loMessageId);
        } catch (Exception e) {
            LOG.error("Cannot send message to IoT Hub", e);
            counterProvider.getMesasageSentFailedCounter().increment();
            loAdapter.sendMessageAck(loMessageId);
        }
    }

    private void handleDeviceCreationEvent(int loMessageId, String message) {
        Optional<String> deviceId = getDeviceId(message);
        deviceId.ifPresent(iotHubAdapter::createOrGetDeviceClientManager);
        loAdapter.sendMessageAck(loMessageId);
    }

    private void handleDeviceRemovalEvent(int loMessageId, String message) {
        Optional<String> deviceId = getDeviceId(message);
        deviceId.ifPresent(iotHubAdapter::deleteDevice);
        loAdapter.sendMessageAck(loMessageId);
    }

    private static String getSourceDeviceId(String msg) throws JSONException {
        return new JSONObject(msg).getJSONObject("metadata").getString("source");
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
            return UNKNOWN_MESSAGE_TYPE;
        }
    }
}