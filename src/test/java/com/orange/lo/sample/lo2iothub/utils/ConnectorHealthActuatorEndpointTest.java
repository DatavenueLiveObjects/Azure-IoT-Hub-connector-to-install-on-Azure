/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectorHealthActuatorEndpointTest {

    private Counters counters;
    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;


    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = mockMeterRegistry();
        this.counters = new Counters(meterRegistry);
        this.connectorHealthActuatorEndpoint = new ConnectorHealthActuatorEndpoint(counters);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void checkCloudConnectionStatus(boolean isConnected) {
        // when
        counters.setCloudConnectionStatus(isConnected);
        boolean cloudConnectionStatus = (boolean) connectorHealthActuatorEndpoint.health().getDetails()
                .get("cloudConnectionStatus");

        // then
        assertEquals(isConnected, cloudConnectionStatus);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void checkLoConnectionStatus(boolean isConnected) {
        // when
        counters.setLoConnectionStatus(isConnected);
        boolean loMqttConnectionStatus = (boolean) connectorHealthActuatorEndpoint.health().getDetails()
                .get("loMqttConnectionStatus");

        // then
        assertEquals(isConnected, loMqttConnectionStatus);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void checkHealth(boolean includeDetails) {
        // when
        Health health = connectorHealthActuatorEndpoint.getHealth(includeDetails);
        Map<String, Object> details = health.getDetails();
        int expectedDetailsSize = includeDetails ? 2 : 0;

        // then
        assertEquals(expectedDetailsSize, details.size());
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(Arguments.of(true),
                Arguments.of(false));
    }

    private MeterRegistry mockMeterRegistry() {
        StepMeterRegistry meterRegistry = mock(StepMeterRegistry.class);
        when(meterRegistry.counter("message.read")).thenReturn(mock(Counter.class));
        when(meterRegistry.counter("message.sent")).thenReturn(mock(Counter.class));
        when(meterRegistry.counter("message.sent.attempt")).thenReturn(mock(Counter.class));
        when(meterRegistry.counter("message.sent.attempt.failed")).thenReturn(mock(Counter.class));
        when(meterRegistry.counter("message.sent.failed")).thenReturn(mock(Counter.class));
        when(meterRegistry.gauge(eq("status.connection.lo"), any())).thenReturn(new AtomicInteger());
        when(meterRegistry.gauge(eq("status.connection.cloud"), any())).thenReturn(new AtomicInteger());

        return meterRegistry;
    }
}