/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.utils;

import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorHealthActuatorEndpointTest {

    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;

    @BeforeEach
    void setUp() {
        this.connectorHealthActuatorEndpoint = new ConnectorHealthActuatorEndpoint();
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void checkConnectionStatus(
            MultiplexingClient multiplexingClient, IotHubConnectionStatus iotHubConnectionStatus, boolean isConnected) {
        // when
        connectorHealthActuatorEndpoint.addMultiplexingConnectionStatus(multiplexingClient, iotHubConnectionStatus);
        boolean cloudConnectionStatus = (boolean) connectorHealthActuatorEndpoint.health().getDetails()
                .get("cloudConnectionStatus");

        // then
        assertEquals(cloudConnectionStatus, isConnected);
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(Arguments.of(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                        IotHubConnectionStatus.CONNECTED, true),
                Arguments.of(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                        IotHubConnectionStatus.DISCONNECTED, false),
                Arguments.of(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                        IotHubConnectionStatus.DISCONNECTED_RETRYING, false));
    }
}