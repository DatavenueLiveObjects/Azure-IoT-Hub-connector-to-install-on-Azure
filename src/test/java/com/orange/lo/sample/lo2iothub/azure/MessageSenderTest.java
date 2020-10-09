package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    public static final String CONNECTION_STRING = "HostName=azure-devices.net;DeviceId=iot-device-id;SharedAccessKey=b3Jhbmdl";

    @Mock
    private Counters counterProvider;
    @Mock
    private Counter counter;
    private DeviceClient deviceClient;
    private MessageSender messageSender;

    @BeforeEach
    void setUp() throws URISyntaxException {
        messageSender = new MessageSender(counterProvider);
        deviceClient = new DeviceClient(CONNECTION_STRING, IotHubClientProtocol.MQTT);
    }

    @Test
    void sendMessage() throws IOException {
//        when(counterProvider.evtAttempt()).thenReturn(counter);
//        Message<String> message = new GenericMessage<>("{\"metadata\":{\"source\":\"iot-device-id\"}}");
//
//        messageSender.sendMessage(message, deviceClient);
//        verify(counterProvider, times(1)).evtAttempt();
    }
}