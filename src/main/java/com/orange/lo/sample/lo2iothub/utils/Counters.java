/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.step.StepMeterRegistry;

@Component
public class Counters {

    private final Counter mesasageReadCounter;
    private final Counter mesasageSentAttemptCounter;
    private final Counter mesasageSentAttemptFailedCounter;
    private final Counter mesasageSentCounter;
    private final Counter mesasageSentFailedCounter;

    public Counters(@Qualifier("counters") StepMeterRegistry registry) {
        mesasageReadCounter = registry.counter("message.read");
        mesasageSentAttemptCounter = registry.counter("message.sent.attempt");
        mesasageSentAttemptFailedCounter = registry.counter("message.sent.attempt.failed");
        mesasageSentCounter = registry.counter("message.sent");
        mesasageSentFailedCounter = registry.counter("message.sent.failed");
        registry.start(Executors.defaultThreadFactory());
    }

    public Counter getMesasageReadCounter() {
        return mesasageReadCounter;
    }

    public Counter getMesasageSentAttemptCounter() {
        return mesasageSentAttemptCounter;
    }

    public Counter getMesasageSentAttemptFailedCounter() {
        return mesasageSentAttemptFailedCounter;
    }

    public Counter getMesasageSentCounter() {
        return mesasageSentCounter;
    }

    public Counter getMesasageSentFailedCounter() {
        return mesasageSentFailedCounter;
    }

    public List<Counter> getAll() {
        return Arrays.asList(mesasageReadCounter, mesasageSentAttemptCounter, mesasageSentAttemptFailedCounter, mesasageSentCounter, mesasageSentFailedCounter);
    }
}