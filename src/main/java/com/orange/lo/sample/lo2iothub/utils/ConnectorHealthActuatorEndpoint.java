package com.orange.lo.sample.lo2iothub.utils;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sdk.LOApiClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class ConnectorHealthActuatorEndpoint implements HealthIndicator {

    private Set<LOApiClient> loApiClients = new HashSet<LOApiClient>();
    private Map<MultiplexingClient, IotHubConnectionStatus> multiplexingClientStatus = new HashMap<>();

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder(Status.UP);
        boolean allConnected = loApiClients.stream().allMatch(c -> c.getDataManagementFifo().isConnected());
        builder.withDetail("loMqttConnectionStatus", allConnected);

        boolean cloudConnectionStatus = multiplexingClientStatus.values()
                .stream().allMatch(cs -> cs.equals(IotHubConnectionStatus.CONNECTED));
        builder.withDetail("cloudConnectionStatus", cloudConnectionStatus);

        return builder.build();
    }

    public void addLoApiClient(LOApiClient loApiClient) {
        this.loApiClients.add(loApiClient);
    }

    public void addMultiplexingConnectionStatus(Map<MultiplexingClient, IotHubConnectionStatus> multiplexingClientStatus) {
        this.multiplexingClientStatus = multiplexingClientStatus;
    }
}