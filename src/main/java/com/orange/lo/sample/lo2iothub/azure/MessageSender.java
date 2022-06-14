/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.orange.lo.sample.lo2iothub.utils.Counters;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;

    public MessageSender(Counters counterProvider) {
        this.counterProvider = counterProvider;
    }

    public void sendMessage(String msg, DeviceClient deviceClient) {
        counterProvider.getMesasageSentAttemptCounter().increment();
        com.microsoft.azure.sdk.iot.device.Message message = new com.microsoft.azure.sdk.iot.device.Message(msg);
        deviceClient.sendEventAsync(message, new EventCallback(), message);
    }

    protected class EventCallback implements IotHubEventCallback {
        @Override
        public void execute(IotHubStatusCode status, Object context) {

            switch (status) {
            case OK:
            case OK_EMPTY:
                counterProvider.getMesasageSentCounter().increment();
                break;
            default:
                counterProvider.getMesasageSentFailedCounter().increment();
                break;
            }

            if (LOG.isDebugEnabled()) {
                com.microsoft.azure.sdk.iot.device.Message msg = (com.microsoft.azure.sdk.iot.device.Message) context;
                LOG.debug("IoT Hub responded to message {} with status {}", msg.getMessageId(), status.name());
            }
        }
    }
}