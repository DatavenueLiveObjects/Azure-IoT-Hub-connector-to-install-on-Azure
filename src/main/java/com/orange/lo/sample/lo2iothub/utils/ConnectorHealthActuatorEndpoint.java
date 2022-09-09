package com.orange.lo.sample.lo2iothub.utils;

import com.orange.lo.sdk.LOApiClient;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class ConnectorHealthActuatorEndpoint implements HealthIndicator {

    private Set<LOApiClient> loApiClients = new HashSet<LOApiClient>();

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder(Status.UP);
        boolean allConnected = loApiClients.stream().allMatch(c -> c.getDataManagementFifo().isConnected());

        builder.withDetail("loMqttConnectionStatus", allConnected);
        return builder.build();
    }

    public void addLoApiClient(LOApiClient loApiClient) {
        this.loApiClients.add(loApiClient);
    }
}