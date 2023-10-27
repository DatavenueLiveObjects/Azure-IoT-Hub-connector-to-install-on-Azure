package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.ConnectionStatusChangeContext;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;

public class MultiplexingClientIotHubConnectionStatusChangeCallback implements IotHubConnectionStatusChangeCallback, MutliplexedConnectionTracker {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MultiplexingClient multiplexingClient;
    private IotHubConnectionStatus multiplexedConnectionStatus;
    private final ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;
    private final int clientNo;

    public MultiplexingClientIotHubConnectionStatusChangeCallback(ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint, MultiplexingClient multiplexingClient, int clientNo) {
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
        this.multiplexingClient = multiplexingClient;
        this.clientNo = clientNo;
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        multiplexedConnectionStatus = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        connectorHealthActuatorEndpoint.addMultiplexingConnectionStatus(multiplexingClient, multiplexedConnectionStatus);

        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            LOG.info("Connection status changed for multiplexing client: {}, status: {}, reason: {}", clientNo, multiplexedConnectionStatus, statusChangeReason);
        } else {
            LOG.error("Connection status changed for multiplexing client: {}, status: {}, reason: {}, error: {}", clientNo, multiplexedConnectionStatus, statusChangeReason, throwable.getMessage());
        }

        if (multiplexedConnectionStatus == IotHubConnectionStatus.DISCONNECTED) {
            new Thread(() -> {
                reconnectMultiplexingClient(multiplexingClient, clientNo);
            }).start();
        }
    }

    private void reconnectMultiplexingClient(MultiplexingClient multiplexingClient, int clientNo) {
        Failsafe.with(getReconnectRetryPolicy()).run(() -> {
            LOG.info("Reconnecting MultiplexingClient nr {} ", clientNo);
            LOG.info("Closing MultiplexingClient nr {} ", clientNo);
            multiplexingClient.close();
            LOG.info("Closing MultiplexingClient nr {} success", clientNo);
            LOG.info("Opening MultiplexingClient nr {}", clientNo);
            multiplexingClient.open(true);
            LOG.info("Opening MultiplexingClient nr {} success", clientNo);
            LOG.info("Reconnecting MultiplexingClient nr {} success", clientNo);
        });
    }

    private RetryPolicy<Void> getReconnectRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> LOG.error("Reconnecting MultiplexingClient nr " + clientNo + " error because of " + e.getLastFailure().getMessage() + ", retrying ...", e.getLastFailure()));
    }

    @Override
    public IotHubConnectionStatus getMultiplexedConnectionStatus() {
        return multiplexedConnectionStatus;
    }
}