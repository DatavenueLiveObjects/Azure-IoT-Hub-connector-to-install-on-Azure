package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.TransportException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.SendMessageException;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceClientManager implements MessageCallback, IotHubConnectionStatusChangeCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";

    private Object reestablishSessionLock = new Object();
    private static final long REESTABLISH_SESSION_DELAY = 10_000; // 10 seconds
    private long lastReestablishSessionTimestamp = 0;

    private static final long MESSAGE_EXPIRY_TIME = 60_000; // 1 minute
    private static final long RETRY_SEND_MESSAGE_DELAY = 10_000; // 10 seconds
    private static final int RETRY_SEND_MESSAGE_MAX_ATTEMPTS = 10;

    private final DeviceClient deviceClient;
    private final LoCommandSender loCommandSender;
    private MultiplexingClientManager multiplexingClientManager;
    private final IoTDeviceProvider ioTDeviceProvider;

    private static final Map<String, MessageSentCallback> callbacksCache = new ConcurrentHashMap<>();

    private Counters counterProvider;
    private RetryPolicy<Void> messageRetryPolicy;
    private Fallback<Void> sendMessageFallback;

    public DeviceClientManager(Device device, String host, LoCommandSender loCommandSender, IoTDeviceProvider ioTDeviceProvider, Counters counterProvider, RetryPolicy<Void> messageRetryPolicy, Fallback<Void> sendMessageFallback) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.deviceClient = new DeviceClient(getConnectionString(device, host), IotHubClientProtocol.AMQPS);
        this.deviceClient.setMessageCallback(this, null);
        this.deviceClient.setConnectionStatusChangeCallback(this, null);
        this.loCommandSender = loCommandSender;
        this.counterProvider = counterProvider;
        this.messageRetryPolicy = messageRetryPolicy;
        this.sendMessageFallback = sendMessageFallback;
    }

    public void setMultiplexingClientManager(MultiplexingClientManager multiplexingClientManager) {
        this.multiplexingClientManager = multiplexingClientManager;
    }

    public DeviceClient getDeviceClient() {
        return deviceClient;
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            LOG.info("Connection status changed for device client: {}, status: {}, reason: {}", deviceClient.getConfig().getDeviceId(), status, statusChangeReason);
        } else {
            LOG.error("Connection status changed for device client: {}, status: {}, reason: {}, error: {} {}", deviceClient.getConfig().getDeviceId(), status, statusChangeReason, throwable.getClass(), throwable.getMessage());
        }

        if (status == IotHubConnectionStatus.DISCONNECTED && statusChangeReason != IotHubConnectionStatusChangeReason.CLIENT_CLOSE) {
            reestablishSessionAsync(throwable);
        }
    }

    private void reestablishSessionAsync(Throwable throwable) {
        new Thread(() -> {
            synchronized (reestablishSessionLock) {
                // This device is always multiplexed so if multiplexing client is reconnecting we do not want to reconnect device client
                if (multiplexingClientManager.getMultiplexedConnectionStatus() == IotHubConnectionStatus.CONNECTED
                        // we also do not want to reconnect device client in loop because of many messages
                        && System.currentTimeMillis() - lastReestablishSessionTimestamp > REESTABLISH_SESSION_DELAY) {
                    TransportException transportExceptionFromThrowable = getTransportExceptionFromThrowable(throwable);

                    Failsafe.with(getReconnectRetryPolicy()).run(() -> {
                        LOG.info("Reconnecting device client: {}", deviceClient.getConfig().getDeviceId());
                        if (multiplexingClientManager.isDeviceRegistered(deviceClient.getConfig().getDeviceId())) {
                            LOG.info("Unregister device client: {}", deviceClient.getConfig().getDeviceId());
                            multiplexingClientManager.unregisterDeviceClient(this);
                            LOG.info("Unregister device client: {} success", deviceClient.getConfig().getDeviceId());
                        }
                        if (transportExceptionFromThrowable.isRetryable() && ioTDeviceProvider.deviceExists(deviceClient.getConfig().getDeviceId())) {
                            LOG.info("Register device client: {}", deviceClient.getConfig().getDeviceId());
                            multiplexingClientManager.registerDeviceClientManager(this);
                            LOG.info("Register device client: {} success", deviceClient.getConfig().getDeviceId());
                        } else if (!transportExceptionFromThrowable.isRetryable()) {
                            LOG.info("Reconnecting device client: {} was abandoned due to encountering a non-retryable exception: {}: {}", deviceClient.getConfig().getDeviceId(), throwable.getClass(), throwable.getMessage());
                        } else {
                            LOG.info("Device client: {} not exists in IoT Hub", deviceClient.getConfig().getDeviceId());
                        }
                        lastReestablishSessionTimestamp = System.currentTimeMillis();
                    });
                }
            }
        }).start();
    }

    private RetryPolicy<Void> getReconnectRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> LOG.error("Reconnecting device client " + deviceClient.getConfig().getDeviceId() + " error because of " + e.getLastFailure().getMessage() + ", retrying ...", e.getLastFailure()));
    }

    @Override
    public IotHubMessageResult onCloudToDeviceMessageReceived(Message message, Object callbackContext) {

        LOG.debug("Received command for device: {} with content {}", deviceClient.getConfig().getDeviceId(),
                new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
        for (MessageProperty messageProperty : message.getProperties()) {
            LOG.debug("{} : {}", messageProperty.getName(), messageProperty.getValue());
        }
        return loCommandSender.send(deviceClient.getConfig().getDeviceId(), new String(message.getBytes()));
    }

    private String getConnectionString(Device device, String host) {
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, host, deviceId, primaryKey);
    }

    public void sendMessage(String loMessage) {
        Message message = new Message(loMessage);
        sendMessage(message);
    }

    private void sendMessage(Message message) {
        MessageSentCallback messageSentCallback = callbacksCache.computeIfAbsent(message.getMessageId(), k -> new MessageSentCallbackImpl());
        try {
            Failsafe.with(sendMessageFallback, messageRetryPolicy).run(() -> {
                counterProvider.getMesasageSentAttemptCounter().increment();
                message.setExpiryTime(MESSAGE_EXPIRY_TIME);
                deviceClient.sendEventAsync(message, messageSentCallback, null);
            });
        } catch (SendMessageException e) {
            LOG.error("Cannot send message with id " + message.getMessageId() + " from " + deviceClient.getConfig().getDeviceId(), e);
            counterProvider.getMesasageSentFailedCounter().increment();
            callbacksCache.remove(message.getMessageId());
        }
    }

    public String getDeviceId() {
        ClientConfiguration config = deviceClient.getConfig();
        return config.getDeviceId();
    }

    @Override
    public String toString() {
        return getDeviceId();
    }

    /**
     * Based on method from {@link com.microsoft.azure.sdk.iot.device.transport.IotHubTransport}
     * @param cause The throwable that caused the change in status. May be null if there wasn't an associated throwable.
     * @return Instance of TransportException
     */
    private static TransportException getTransportExceptionFromThrowable(Throwable cause) {
        TransportException transportException;
        if (cause instanceof TransportException) {
            return (TransportException) cause;
        }
        transportException = new TransportException(cause);
        transportException.setRetryable(true);
        return transportException;
    }

    private class MessageSentCallbackImpl implements MessageSentCallback {
        private int actualRetryCount = 1;

        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context) {
            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();

            if (status == IotHubStatusCode.OK) {
                LOG.debug("IoT Hub responded to message with id {} from {} with status {}", sentMessage.getMessageId(), deviceClient.getConfig().getDeviceId(), status.name());
                callbacksCache.remove(sentMessage.getMessageId());
                counterProvider.getMesasageSentCounter().increment();
            } else {
                counterProvider.getMesasageSentAttemptFailedCounter().increment();
                if (exception.isRetryable() && actualRetryCount < RETRY_SEND_MESSAGE_MAX_ATTEMPTS) {
                    LOG.debug("IoT Hub responded to message with id {} from {} with status {}. Retrying...", sentMessage.getMessageId(), deviceClient.getConfig().getDeviceId(), status.name());
                    actualRetryCount++;
                    reestablishSessionAsync(exception);
                    goSleep();
                    sendMessage(sentMessage);
                } else {
                    LOG.error("IoT Hub responded to message with id {} from {} with status {}. Message will not be sent again.", sentMessage.getMessageId(), deviceClient.getConfig().getDeviceId(), status.name());
                    callbacksCache.remove(sentMessage.getMessageId());
                    counterProvider.getMesasageSentFailedCounter().increment();
                }
            }
        }

        private void goSleep() {
            try {
                Thread.sleep(RETRY_SEND_MESSAGE_DELAY);
            } catch (InterruptedException e) {
            }
        }
    }
}