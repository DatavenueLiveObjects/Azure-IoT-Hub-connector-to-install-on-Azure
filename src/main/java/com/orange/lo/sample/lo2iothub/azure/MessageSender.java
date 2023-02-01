/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
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
        deviceClient.sendEventAsync(message, new MessageSentCallbackImpl(), message);
    }

    protected class MessageSentCallbackImpl implements MessageSentCallback {

        @Override
        public void onMessageSent(Message sentMessage, IotHubClientException clientException, Object context) {
            switch (clientException.getStatusCode()) {
            case OK:
                counterProvider.getMesasageSentCounter().increment();
                break;
            default:
                counterProvider.getMesasageSentFailedCounter().increment();
                break;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("IoT Hub responded to message {} with status {}", sentMessage.getMessageId(),
                        clientException.getStatusCode().name());
            }
        }
    }
}