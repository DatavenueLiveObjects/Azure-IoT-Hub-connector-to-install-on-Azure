/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;

@Component
public class Counters {

    private final Counter mesasageReadCounter;
    private final Counter mesasageSentAttemptCounter;
    private final Counter mesasageSentAttemptFailedCounter;
    private final Counter mesasageSentCounter;
    private final Counter mesasageSentFailedCounter;
    private AtomicInteger loConnectionStatus;
    private AtomicInteger cloudConnectionStatus;

    public Counters(@Qualifier("counters") MeterRegistry meterRegistry) {
        mesasageReadCounter = meterRegistry.counter("message.read");
        mesasageSentAttemptCounter = meterRegistry.counter("message.sent.attempt");
        mesasageSentAttemptFailedCounter = meterRegistry.counter("message.sent.attempt.failed");
        mesasageSentCounter = meterRegistry.counter("message.sent");
        mesasageSentFailedCounter = meterRegistry.counter("message.sent.failed");
        loConnectionStatus = meterRegistry.gauge("status.connection.lo", new AtomicInteger(1));
        cloudConnectionStatus = meterRegistry.gauge("status.connection.cloud", new AtomicInteger(1));
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

    public void setLoConnectionStatus(boolean status) {
        loConnectionStatus.set(status ? 1 : 0);
    }

    public void setCloudConnectionStatus(boolean status) {
        cloudConnectionStatus.set(status ? 1 : 0);
    }

    public boolean isCloudConnectionStatusUp() {
        return cloudConnectionStatus.get() > 0;
    }

    public boolean isLoConnectionStatusUp() {
        return loConnectionStatus.get() > 0;
    }

    public List<Counter> getAll() {
        return Arrays.asList(mesasageReadCounter, mesasageSentAttemptCounter, mesasageSentAttemptFailedCounter, mesasageSentCounter, mesasageSentFailedCounter);
    }
}