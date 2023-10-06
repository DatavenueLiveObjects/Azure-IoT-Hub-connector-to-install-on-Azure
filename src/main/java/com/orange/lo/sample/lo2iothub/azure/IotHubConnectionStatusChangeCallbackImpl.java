package com.orange.lo.sample.lo2iothub.azure;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.sdk.iot.device.ConnectionStatusChangeContext;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;

public class IotHubConnectionStatusChangeCallbackImpl implements IotHubConnectionStatusChangeCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private MultiplexingClient multiplexingClient;
    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;
    public Map<MultiplexingClient, IotHubConnectionStatus> multiplexingClientStatus = new HashMap<>();
    int clientNo;

    public IotHubConnectionStatusChangeCallbackImpl(ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint, MultiplexingClient multiplexingClient, int clientNo) {
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
        this.multiplexingClient = multiplexingClient;
        this.clientNo = clientNo;
    }

    @Override public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();

        multiplexingClientStatus.put(multiplexingClient, status);
        connectorHealthActuatorEndpoint.addMultiplexingConnectionStatus(multiplexingClientStatus);

        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            LOG.info("Connection status changed for client: {}, status: {}, reason: {}", clientNo, status, statusChangeReason);
        } else {
            LOG.info("Connection status changed for client: {}, status: {}, reason: {}, error: {}", clientNo, status, statusChangeReason, throwable.getMessage());
        }

        if (status == IotHubConnectionStatus.DISCONNECTED) {
            reconnectMultiplexingClient(multiplexingClient, clientNo);
        }
    }

    private void connect(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Opening MultiplexingClient nr {}", clientNo);

        Failsafe.with(getRetryPolicy(clientNo)).run(() -> {
            multiplexingClient.open(true);
            LOG.info("Opening MultiplexingClient nr {} success", clientNo);
        });
    }

    private void reconnectMultiplexingClient(MultiplexingClient multiplexingClient, int clientNo) {
        LOG.info("Reconnecting MultiplexingClient nr {} " + clientNo);

        try {
            LOG.info("Closing MultiplexingClient nr {} " + clientNo);
            multiplexingClient.close();
            connect(multiplexingClient, clientNo);
            LOG.info("Closing MultiplexingClient nr {} success" + clientNo);
        } catch (Exception ex) {
            LOG.error("Reconnecting MultiplexingClient nr {} error because of {}", clientNo, ex.getMessage());
        }
    }

    private RetryPolicy<Void> getRetryPolicy(int clientNo) {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> {
                    LOG.info("Opening MultiplexingClient nr {} error because of {}, retrying ...", clientNo, e.getLastFailure().getMessage());
                });
    }
}
