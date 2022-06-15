/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;

@SpringBootApplication
public class ConnectorApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        SpringApplication.run(ConnectorApplication.class, args);
    }

    @Bean
    public StepRegistryConfig stepRegistryConfig() {
        return new StepRegistryConfig() {

            @Override
            public Duration step() {
                return Duration.ofMinutes(1);
            }

            @Override
            public String prefix() {
                return "";
            }

            @Override
            public String get(String key) {
                return null;
            }
        };
    }

    @Bean
    @Qualifier("counters")
    public StepMeterRegistry stepMeterRegistry() {
        return new StepMeterRegistry(stepRegistryConfig(), Clock.SYSTEM) {

            @Override
            protected TimeUnit getBaseTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            protected void publish() {
                getMeters().stream().filter(m -> m.getId().getName().startsWith("message")).map(m -> get(m.getId().getName()).counter()).forEach(c -> LOG.info(c.getId().getName() + " = " + val(c)));
            }

            @Override
            public void start(ThreadFactory threadFactory) {
                super.start(Executors.defaultThreadFactory());
            }
        };
    }

    private long val(Counter cnt) {
        return Math.round(cnt.count());
    }
}
