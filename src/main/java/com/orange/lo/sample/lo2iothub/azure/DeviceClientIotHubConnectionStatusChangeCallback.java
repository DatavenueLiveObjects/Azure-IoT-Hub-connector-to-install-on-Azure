package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;

public class DeviceClientIotHubConnectionStatusChangeCallback implements IotHubConnectionStatusChangeCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DeviceClient deviceClient;
    private final MultiplexingClient multiplexingClient;
    private final MutliplexedConnectionTracker mutliplexedConnectionTracker;

    public DeviceClientIotHubConnectionStatusChangeCallback(DeviceClient deviceClient, MultiplexingClient multiplexingClient, MutliplexedConnectionTracker mutliplexedConnectionTracker) {
        this.deviceClient = deviceClient;
        this.multiplexingClient = multiplexingClient;
        this.mutliplexedConnectionTracker = mutliplexedConnectionTracker;
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            LOG.info("Connection status changed for device client: {}, status: {}, reason: {}", deviceClient.getConfig().getDeviceId(), status, statusChangeReason);
        } else {
            LOG.error("Connection status changed for device client: {}, status: {}, reason: {}, error: {}", deviceClient.getConfig().getDeviceId(), status, statusChangeReason, throwable.getMessage());
        }

        // This device is always multiplexed so if multiplexing client is reconnecting we do not want to reconnect device client
        if (status == IotHubConnectionStatus.DISCONNECTED && mutliplexedConnectionTracker.getMultiplexedConnectionStatus() == IotHubConnectionStatus.CONNECTED) {
            new Thread(() -> {
                reestablishSession();
            }).start();
        }
    }

    private void reestablishSession() {
        Failsafe.with(getReconnectRetryPolicy()).run(() -> {
            // check 2nd time because here we are in different thread and multiplexing client could be disconnected
            if (mutliplexedConnectionTracker.getMultiplexedConnectionStatus() == IotHubConnectionStatus.CONNECTED) {
                LOG.info("Reconnecting device client: {}", deviceClient.getConfig().getDeviceId());
                if (multiplexingClient.isDeviceRegistered(deviceClient.getConfig().getDeviceId())) {
                    LOG.info("Unregister device client: {}", deviceClient.getConfig().getDeviceId());
                    multiplexingClient.unregisterDeviceClient(deviceClient);
                    LOG.info("Unregister device client: {} success", deviceClient.getConfig().getDeviceId());
                }
                LOG.info("Register device client: {}", deviceClient.getConfig().getDeviceId());
                multiplexingClient.registerDeviceClient(deviceClient);
                LOG.info("Register device client: {} success", deviceClient.getConfig().getDeviceId());
            }
        });
    }

    private RetryPolicy<Void> getReconnectRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> LOG.error("Reconnecting device client " + deviceClient.getConfig().getDeviceId() + " error because of " + e.getLastFailure().getMessage() + ", retrying ...", e.getLastFailure()));
    }
}
