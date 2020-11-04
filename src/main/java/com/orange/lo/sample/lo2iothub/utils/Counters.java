/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Counters {

    private Counter evtReceived;
    private Counter evtAttempt;
    private Counter evtSent;
    private Counter evtFailed;

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
