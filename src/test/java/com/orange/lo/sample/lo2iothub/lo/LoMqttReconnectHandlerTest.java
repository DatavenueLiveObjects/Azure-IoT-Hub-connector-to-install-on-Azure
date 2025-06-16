package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.utils.Counters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoMqttReconnectHandlerTest {

    private Counters counters;
    private LoMqttReconnectHandler loMqttReconnectHandler;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = mockMeterRegistry();
        this.counters = new Counters(meterRegistry);
        this.loMqttReconnectHandler = new LoMqttReconnectHandler(counters);
    }

    @Test
    void shouldChangeLoConnectionStausWhenConnectComplete() {
        loMqttReconnectHandler.connectComplete(false, "");

        assertTrue(counters.isLoConnectionStatusUp());
    }

    @Test
    void shouldChangeLoConnectionStausWhenConnectionLost() {
        loMqttReconnectHandler.connectionLost(new Exception());

        assertFalse(counters.isLoConnectionStatusUp());
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