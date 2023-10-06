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

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

public class MessageSender implements CacheExpiredListener {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;
    private RetryPolicy<Void> messageRetryPolicy;
    private static final Map<String, MessageSentCallback> callbacksCache = new ConcurrentHashMap<>();
    private MessagesCache messagesCache;

    public MessageSender(Counters counterProvider) {
        this.counterProvider = counterProvider;
    }

    public void sendMessage(LoMessageDetails loMessageDetails) {
        messagesCache.put(loMessageDetails.getMessageId(), loMessageDetails);
        MessageSentCallback messageSentCallback = callbacksCache.computeIfAbsent(loMessageDetails.getMessageId(), k -> new MessageSentCallbackImpl());

        try {
            Fallback<Void> objectFallback = Fallback.ofException(e -> new SendMessageException(e.getLastFailure()));
            Failsafe.with(objectFallback, messageRetryPolicy).run(() -> {
                counterProvider.getMesasageSentAttemptCounter().increment();
                Message message = new Message(loMessageDetails.getMessage());
                loMessageDetails.getIoTHubClient().getDeviceClient().sendEventAsync(message, messageSentCallback, loMessageDetails);
            });
        } catch (SendMessageException e) {
            LOG.error("Cannot send message created " + loMessageDetails.getMessageCreated() + " from " + loMessageDetails.getDeviceId(), e);
            counterProvider.getMesasageSentFailedCounter().increment();
            callbacksCache.remove(loMessageDetails.getMessageId());
            messagesCache.invalidate(loMessageDetails.getMessageId());
        }
    }

    @Override
    public void onExpired(String key, LoMessageDetails loMessageDetails) {
        // Reconnect multiplex client. It will be recreated if it was closed.
        LOG.debug("Message {} expired. Reconnecting multiplex client for device {}", loMessageDetails.getMessageId(), loMessageDetails.getDeviceId());
        loMessageDetails.getIoTHubClient().getMultiplexingClient().close();
    }

    private class MessageSentCallbackImpl implements MessageSentCallback {
        private static final int MAX_ATTEMPTS = 5;
        private int actualRetryCount = 1;

        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context) {
            LoMessageDetails loMessageDetails = (LoMessageDetails) context;

            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
            if (status == IotHubStatusCode.OK) {
                callbacksCache.remove(loMessageDetails.getMessageId());
                messagesCache.invalidate(loMessageDetails.getMessageId());
                counterProvider.getMesasageSentCounter().increment();
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
                    messagesCache.invalidate(loMessageDetails.getMessageId());
                    counterProvider.getMesasageSentFailedCounter().increment();
                    LOG.error("IoT Hub responded to message created {} from {} with status {}. Message will not be sent again.", loMessageDetails.getMessageCreated(), loMessageDetails.getDeviceId(), status.name());
                }
            }
        }

        private void goSleep(){
            try {
                Thread.sleep(1000 * (1<<actualRetryCount));
            } catch (InterruptedException e) {}
        }
    }

    public void setMessagesCache(MessagesCache messagesCache) {
        this.messagesCache = messagesCache;
    }

    public void setMessageRetryPolicy(RetryPolicy<Void> messageRetryPolicy) {
        this.messageRetryPolicy = messageRetryPolicy;
    }
}