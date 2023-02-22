/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.orange.lo.sample.lo2iothub.exceptions.SendMessageException;
import com.orange.lo.sample.lo2iothub.utils.Counters;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

@Component
public class MessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;
    private RetryPolicy<Void> messageRetryPolicy;

    public MessageSender(Counters counterProvider) {
        this.counterProvider = counterProvider;
    }

    public void setMessageRetryPolicy(RetryPolicy<Void> messageRetryPolicy) {
        this.messageRetryPolicy = messageRetryPolicy;
    }

    public void sendMessage(String msg, DeviceClient deviceClient) {
        try {
            Fallback<Void> objectFallback = Fallback.ofException(e -> new SendMessageException(e.getLastFailure()));
            Failsafe.with(objectFallback, messageRetryPolicy).run(() -> {
                counterProvider.getMesasageSentAttemptCounter().increment();
                Message message = new Message(msg);
                deviceClient.sendEventAsync(message, new TelemetryAcknowledgedEventCallback(), message);
            });
        } catch (SendMessageException e) {
            LOG.error("Cannot send message", e);
            counterProvider.getMesasageSentFailedCounter().increment();
        }
    }

    private class TelemetryAcknowledgedEventCallback implements MessageSentCallback {

        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context) {
            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
            if (status == IotHubStatusCode.OK) {
                counterProvider.getMesasageSentCounter().increment();
            } else {
                counterProvider.getMesasageSentFailedCounter().increment();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("IoT Hub responded to message {} with status {}", sentMessage.getMessageId(), status.name());
            }
        }
    }
}