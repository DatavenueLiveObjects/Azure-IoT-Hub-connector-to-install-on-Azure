/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.TransportException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DeviceClientManager implements MessageCallback, IotHubConnectionStatusChangeCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";

    private Object reestablishSessionLock = new Object();
    private long lastReestablishSessionTimestamp = 0;

    private final DeviceClient deviceClient;
    private final LoCommandSender loCommandSender;
    private MultiplexingClientManager multiplexingClientManager;
    private final AzureIotHubProperties azureIotHubProperties;
    private final IoTDeviceProvider ioTDeviceProvider;

    private Counters counterProvider;

    public DeviceClientManager(Device device, AzureIotHubProperties azureIotHubProperties, LoCommandSender loCommandSender, IoTDeviceProvider ioTDeviceProvider, Counters counterProvider) {
        this.azureIotHubProperties = azureIotHubProperties;
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.deviceClient = new DeviceClient(getConnectionString(device, azureIotHubProperties.getIotHostName()), IotHubClientProtocol.AMQPS);
        this.deviceClient.setMessageCallback(this, null);
        this.deviceClient.setConnectionStatusChangeCallback(this, null);
        this.loCommandSender = loCommandSender;
        this.counterProvider = counterProvider;
    }

    @Override
    public IotHubMessageResult onCloudToDeviceMessageReceived(Message message, Object callbackContext) {

        LOG.debug("Received command for device: {} with content {}", getDeviceId(),
                new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
        for (MessageProperty messageProperty : message.getProperties()) {
            LOG.debug("{} : {}", messageProperty.getName(), messageProperty.getValue());
        }
        return loCommandSender.send(getDeviceId(), new String(message.getBytes()));
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            LOG.info("Connection status changed for device client: {}, status: {}, reason: {}", getDeviceId(), status, statusChangeReason);
        } else {
            LOG.error("Connection status changed for device client: {}, status: {}, reason: {}, error: {} {}", getDeviceId(), status, statusChangeReason, throwable.getClass(), throwable.getMessage());
        }

        if (status == IotHubConnectionStatus.DISCONNECTED && statusChangeReason != IotHubConnectionStatusChangeReason.CLIENT_CLOSE) {
            reestablishSessionAsync(throwable);
        }
    }

    public void sendMessage(String loMessage) {
        Message message = new Message(loMessage);
        sendMessage(message);
    }

    private void reestablishSessionAsync(Throwable throwable) {
        new Thread(() -> {
            synchronized (reestablishSessionLock) {
                // This device is always multiplexed so if multiplexing client is reconnecting we do not want to reconnect device client
                if (multiplexingClientManager.getMultiplexedConnectionStatus() == IotHubConnectionStatus.CONNECTED
                        // we also do not want to reconnect device client in loop because of many messages
                        && System.currentTimeMillis() - lastReestablishSessionTimestamp > azureIotHubProperties.getDeviceReestablishSessionDelay()) {
                    TransportException transportExceptionFromThrowable = getTransportExceptionFromThrowable(throwable);

                    Failsafe.with(getReconnectRetryPolicy()).run(() -> {
                        LOG.info("Reconnecting device client: {}", getDeviceId());
                        if (multiplexingClientManager.isDeviceRegistered(getDeviceId())) {
                            LOG.info("Unregister device client: {}", getDeviceId());
                            multiplexingClientManager.unregisterDeviceClient(this);
                            LOG.info("Unregister device client: {} success", getDeviceId());
                        }
                        if (transportExceptionFromThrowable.isRetryable() && ioTDeviceProvider.deviceExists(getDeviceId())) {
                            LOG.info("Register device client: {}", getDeviceId());
                            multiplexingClientManager.registerDeviceClientManager(this);
                            LOG.info("Register device client: {} success", getDeviceId());
                        } else if (!transportExceptionFromThrowable.isRetryable()) {
                            LOG.info("Reconnecting device client: {} was abandoned due to encountering a non-retryable exception: {}: {}", getDeviceId(), throwable.getClass(), throwable.getMessage());
                        } else {
                            LOG.info("Device client: {} not exists in IoT Hub", getDeviceId());
                        }
                        lastReestablishSessionTimestamp = System.currentTimeMillis();
                    });
                }
            }
        }).start();
    }

    private void sendMessage(Message message) {
        Failsafe.with(
            new RetryPolicy<IotHubStatusCode>()
                .withMaxAttempts(azureIotHubProperties.getMessageSendMaxAttempts())
                .withDelay(Duration.ofMillis(azureIotHubProperties.getMessageResendDelay()))
                .handleResultIf(r -> IotHubStatusCode.OK != r)
                .abortOn(e -> e instanceof TransportException && !((TransportException) e).isRetryable())
                .onRetryScheduled(e -> reestablishSessionAsync(e.getLastFailure()))
                .onRetry(r->{
                    LOG.debug("IoT Hub responded to message with id {} from {} with status {}. Retrying...", message.getMessageId(), getDeviceId(), r.getLastResult().name());
                    counterProvider.getMesasageSentAttemptFailedCounter().increment();
                })
                .onSuccess(r -> {
                    LOG.debug("IoT Hub responded to message with id {} from {} with status {}", message.getMessageId(), getDeviceId(), r.getResult().name());
                    counterProvider.getMesasageSentCounter().increment();
                })
                .onFailure(r -> {
                    LOG.error("Cannot send message with id " + message.getMessageId() + " from " + getDeviceId(), r.getFailure());
                    counterProvider.getMesasageSentFailedCounter().increment();
                })
        ).getAsyncExecution(execution -> {
            counterProvider.getMesasageSentAttemptCounter().increment();
            message.setExpiryTime(azureIotHubProperties.getMessageExpiryTime());
            deviceClient.sendEventAsync(message, new MessageSentCallback() {
                @Override
                public void onMessageSent(Message message, IotHubClientException exception, Object context) {
                    IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
                    execution.retryFor(status, exception);
                }
            }, null);
        });
    }

    private RetryPolicy<Void> getReconnectRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> LOG.error("Reconnecting device client " + getDeviceId() + " error because of " + e.getLastFailure().getMessage() + ", retrying ...", e.getLastFailure()));
    }

    private String getConnectionString(Device device, String host) {
        String deviceId = device.getDeviceId();
        String primaryKey = device.getSymmetricKey().getPrimaryKey();
        return String.format(CONNECTION_STRING_PATTERN, host, deviceId, primaryKey);
    }

    /**
     * Based on method from {@link com.microsoft.azure.sdk.iot.device.transport.IotHubTransport}
     * @param cause The throwable that caused the change in status. May be null if there wasn't an associated throwable.
     * @return Instance of TransportException
     */
    private static TransportException getTransportExceptionFromThrowable(Throwable cause) {
        if (cause instanceof TransportException) {
            return (TransportException) cause;
        }
        TransportException transportException = new TransportException(cause);
        transportException.setRetryable(true);
        return transportException;
    }

    @Override
    public String toString() {
        return getDeviceId();
    }

    public String getDeviceId() {
        return deviceClient.getConfig().getDeviceId();
    }

    public void setMultiplexingClientManager(MultiplexingClientManager multiplexingClientManager) {
        this.multiplexingClientManager = multiplexingClientManager;
    }

    public DeviceClient getDeviceClient() {
        return deviceClient;
    }
}