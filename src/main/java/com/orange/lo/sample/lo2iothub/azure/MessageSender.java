/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.orange.lo.sample.lo2iothub.exceptions.SendMessageException;
import com.orange.lo.sample.lo2iothub.utils.Counters;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<String, MessageSentCallback> callbacksCache = new ConcurrentHashMap<>();

    public MessageSender(Counters counterProvider) {
        this.counterProvider = counterProvider;
    }

    public void setMessageRetryPolicy(RetryPolicy<Void> messageRetryPolicy) {
        this.messageRetryPolicy = messageRetryPolicy;
    }

    public void sendMessage(LoMessageDetails loMessageDetails) {
        MessageSentCallback messageSentCallback = callbacksCache.computeIfAbsent(loMessageDetails.getMessageId(), k -> new TelemetryAcknowledgedEventCallback());

        try {
            Fallback<Void> objectFallback = Fallback.ofException(e -> new SendMessageException(e.getLastFailure()));
            Failsafe.with(objectFallback, messageRetryPolicy).run(() -> {
                counterProvider.getMesasageSentAttemptCounter().increment();
                Message message = new Message(loMessageDetails.getMessage());
                loMessageDetails.getDeviceClient().sendEventAsync(message, messageSentCallback, loMessageDetails);
            });
        } catch (SendMessageException e) {
            LOG.error("Cannot send message created " + loMessageDetails.getMessageCreated() + " from " + loMessageDetails.getDeviceId(), e);
            counterProvider.getMesasageSentFailedCounter().increment();
            callbacksCache.remove(loMessageDetails.getMessageId());
        }
    }

    private class TelemetryAcknowledgedEventCallback implements MessageSentCallback {
        private static final int MAX_ATTEMPTS = 5;
        private int actualRetryCount = 1;

        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context) {
            LoMessageDetails loMessageDetails = (LoMessageDetails) context;

            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
            if (status == IotHubStatusCode.OK) {
                counterProvider.getMesasageSentCounter().increment();
                callbacksCache.remove(loMessageDetails.getMessageId());
                LOG.debug("IoT Hub responded to message created {} from {} with status {}", loMessageDetails.getMessageCreated(), loMessageDetails.getDeviceId(), status.name());
            } else {
                counterProvider.getMesasageSentAttemptFailedCounter().increment();
                if (actualRetryCount < MAX_ATTEMPTS) {
                    actualRetryCount++;
                    LOG.debug("IoT Hub responded to message created {} from {} with status {}. Retrying...", loMessageDetails.getMessageCreated(), loMessageDetails.getDeviceId(), status.name());
                    goSleep();
                    sendMessage(loMessageDetails);
                } else {
                    callbacksCache.remove(loMessageDetails.getMessageId());
                    counterProvider.getMesasageSentFailedCounter().increment();
                    LOG.error("IoT Hub responded to message created {} from {} with status {}. Message will not be sent again.", loMessageDetails.getMessageCreated(), loMessageDetails.getDeviceId(), status.name());
                }
            }
        }

        private void goSleep(){
            try {
                Thread.sleep(100 * actualRetryCount);
            } catch (InterruptedException e) {}
        }
    }
}