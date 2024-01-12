/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.utils;

import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sdk.LOApiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ConnectorHealthActuatorEndpoint implements HealthIndicator {

    private final Set<LOApiClient> loApiClients = new HashSet<>();
    private final Map<MultiplexingClient, IotHubConnectionStatus> multiplexingClientStatus = new HashMap<>();

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

    public void addMultiplexingConnectionStatus(MultiplexingClient multiplexingClient, IotHubConnectionStatus iotHubConnectionStatus) {
        this.multiplexingClientStatus.put(multiplexingClient, iotHubConnectionStatus);
    }
}