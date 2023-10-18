package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.ConnectionStatusChangeContext;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IotHubConnectionStatusChangeCallbackImplTest {

    private IotHubConnectionStatusChangeCallbackImpl iotHubConnectionStatusChangeCallback;
    ConnectionStatusChangeContext connectionStatusChangeContext;
    @Mock
    ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;

    @BeforeEach
    void setUp() {
        MultiplexingClient multiplexingClient = new MultiplexingClient("liveobjects.orange-business.com",
                IotHubClientProtocol.AMQPS);

        this.iotHubConnectionStatusChangeCallback = new IotHubConnectionStatusChangeCallbackImpl(
                connectorHealthActuatorEndpoint, multiplexingClient, 1);

        connectionStatusChangeContext = new ConnectionStatusChangeContext(IotHubConnectionStatus.CONNECTED,
                IotHubConnectionStatus.DISCONNECTED, IotHubConnectionStatusChangeReason.CONNECTION_OK, null, null);
    }

    @Test
    void isStatusChanged() {
        CountDownLatch countDownLatch = new CountDownLatch(4);
        doAnswer(invocation -> {
            countDownLatch.countDown();
            return null;
        }).when(connectorHealthActuatorEndpoint).addMultiplexingConnectionStatus(any(), eq(IotHubConnectionStatus.CONNECTED));

        // when
        iotHubConnectionStatusChangeCallback.onStatusChanged(connectionStatusChangeContext);

        // then
        verify(connectorHealthActuatorEndpoint, times(1)).addMultiplexingConnectionStatus(any(), eq(IotHubConnectionStatus.CONNECTED));
    }

}