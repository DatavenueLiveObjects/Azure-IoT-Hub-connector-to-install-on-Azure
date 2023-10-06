package com.orange.lo.sample.lo2iothub.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorHealthActuatorEndpointTest {

    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;

    @BeforeEach void setUp() {
        this.connectorHealthActuatorEndpoint = new ConnectorHealthActuatorEndpoint();
    }

    @ParameterizedTest @MethodSource("provideTestData") void checkConnectionStatus(
            Map<MultiplexingClient, IotHubConnectionStatus> multiplexingClientStatus, boolean isConnected) {
        // when
        connectorHealthActuatorEndpoint.addMultiplexingConnectionStatus(multiplexingClientStatus);
        boolean cloudConnectionStatus = (boolean) connectorHealthActuatorEndpoint.health().getDetails()
                .get("cloudConnectionStatus");

        // then
        assertEquals(cloudConnectionStatus, isConnected);
    }

    private static Stream<Arguments> provideTestData() {
        Map<MultiplexingClient, IotHubConnectionStatus> allConnected = new HashMap<>();
        allConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.CONNECTED);
        allConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.CONNECTED);
        allConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.CONNECTED);

        Map<MultiplexingClient, IotHubConnectionStatus> allNotConnected = new HashMap<>();
        allNotConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.DISCONNECTED);
        allNotConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.DISCONNECTED);
        allNotConnected.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.DISCONNECTED);

        Map<MultiplexingClient, IotHubConnectionStatus> variousConnections = new HashMap<>();
        variousConnections.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.CONNECTED);
        variousConnections.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.DISCONNECTED);
        variousConnections.put(new MultiplexingClient("liveobjects.orange-business.com", IotHubClientProtocol.AMQPS),
                IotHubConnectionStatus.DISCONNECTED_RETRYING);

        return Stream.of(Arguments.of(allConnected, true), Arguments.of(allNotConnected, false),
                Arguments.of(variousConnections, false));
    }
}