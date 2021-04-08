package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private Counters counterProvider;
    @Mock
    private Counter counter;
    @Mock
    private DeviceClient deviceClient;
    private MessageSender messageSender;

    @BeforeEach
    void setUp() {
        messageSender = new MessageSender(counterProvider);
    }

    @Test
    void sendMessage() {
        when(counterProvider.evtAttempt()).thenReturn(counter);
        String message = "{\"metadata\":{\"source\":\"iot-device-id\"}}";

        messageSender.sendMessage(message, deviceClient);
        verify(counterProvider, times(1)).evtAttempt();
    }
}