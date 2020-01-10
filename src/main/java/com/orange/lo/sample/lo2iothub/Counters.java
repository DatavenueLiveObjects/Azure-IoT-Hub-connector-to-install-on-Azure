package com.orange.lo.sample.lo2iothub;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Counters {
    private Counter evtReceived, evtAttempt, evtSent, evtFailed;

    @Autowired
    public Counters(MeterRegistry registry) {
        evtReceived = registry.counter("evt-received");
        evtAttempt = registry.counter("evt-attempt");
        evtSent = registry.counter("evt-sent");
        evtFailed = registry.counter("evt-failed");
    }

    public Counter evtReceived() {
        return evtReceived;
    }

    public Counter evtAttempt() {
        return evtAttempt;
    }

    public Counter evtSent() {
        return evtSent;
    }

    public Counter evtFailed() {
        return evtFailed;
    }
}
