/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.noop.NoopCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountersTest {

    @Mock
    private MeterRegistry registry;
    private Counter evtReceivedStub;
    private Counter evtAttemptStub;
    private Counter evtSentStub;
    private Counter evtFailedStub;
    private Counters counters;

    @BeforeEach
    void setUp() {
        evtReceivedStub = getCounter("evt-received");
        when(registry.counter("evt-received")).thenReturn(evtReceivedStub);
        evtAttemptStub = getCounter("evt-attempt");
        when(registry.counter("evt-attempt")).thenReturn(evtAttemptStub);
        evtSentStub = getCounter("evt-sent");
        when(registry.counter("evt-sent")).thenReturn(evtSentStub);
        evtFailedStub = getCounter("evt-failed");
        when(registry.counter("evt-failed")).thenReturn(evtFailedStub);

        this.counters = new Counters(registry);
    }

    @Test
    void shouldReturnCorrectCounterWhenEventReceivedCounterIsExpected() {
        Counter counter = counters.evtReceived();
        assertEquals(evtReceivedStub, counter);
    }

    @Test
    void shouldReturnCorrectCounterWhenEventAttemptCounterIsExpected() {
        Counter counter = counters.evtAttempt();
        assertEquals(evtAttemptStub, counter);
    }

    @Test
    void shouldReturnCorrectCounterWhenEventSentCounterIsExpected() {
        Counter counter = counters.evtSent();
        assertEquals(evtSentStub, counter);
    }

    @Test
    void shouldReturnCorrectCounterWhenEventFailedCounterIsExpected() {
        Counter counter = counters.evtFailed();
        assertEquals(evtFailedStub, counter);
    }

    private Counter getCounter(String name) {
        Tags tags = Tags.of(new ArrayList<>());
        Meter.Id id = new Meter.Id(name, tags, null, null, Meter.Type.COUNTER);
        return new NoopCounter(id);
    }
}