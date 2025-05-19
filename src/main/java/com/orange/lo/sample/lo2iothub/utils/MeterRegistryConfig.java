/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.utils;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Configuration
public class MeterRegistryConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String AWS_SERVICE_PROFILE_NAME = "service-profile";
    private final MetricsProperties metricsProperties;

    public MeterRegistryConfig(MetricsProperties metricsProperties) {
        this.metricsProperties = metricsProperties;
    }

    @Bean
    @Qualifier("counters")
    public MeterRegistry meterRegistry() {
        MeterRegistry meterRegistry;
        if (metricsProperties.isSendToCloudwatch()) {
            meterRegistry = getCloudWatchMeterRegistry();
        } else {
            meterRegistry = stepMeterRegistry();
        }
        return meterRegistry;
    }

    private CloudWatchMeterRegistry getCloudWatchMeterRegistry() {
        CloudWatchAsyncClient cloudWatchAsyncClient = CloudWatchAsyncClient.builder()
                .credentialsProvider(getAwsCredentialsProvider())
                .region(getRegion())
                .build();

        CloudWatchMeterRegistry cloudWatchMeterRegistry = new CloudWatchMeterRegistry(cloudWatchConfig(), Clock.SYSTEM, cloudWatchAsyncClient);

        cloudWatchMeterRegistry.config()
                .meterFilter(MeterFilter.deny(id -> !id.getName().startsWith("message")))
                .commonTags(metricsProperties.getDimensionName(), metricsProperties.getDimensionValue());
        return cloudWatchMeterRegistry;
    }

    private AwsCredentialsProvider getAwsCredentialsProvider() {
        return metricsProperties.isUseServiceProfile()
                ? ProfileCredentialsProvider.create(AWS_SERVICE_PROFILE_NAME)
                : InstanceProfileCredentialsProvider.create();
    }

    private Region getRegion() {
        return metricsProperties.isUseServiceProfile()
                ? new AwsProfileRegionProvider(null, AWS_SERVICE_PROFILE_NAME).getRegion()
                : new InstanceProfileRegionProvider().getRegion();
    }

    private CloudWatchConfig cloudWatchConfig() {
        return new CloudWatchConfig() {

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String namespace() {
                return metricsProperties.getNamespace();
            }
        };
    }

    private StepMeterRegistry stepMeterRegistry() {
        StepMeterRegistry stepMeterRegistry = new StepMeterRegistry(stepRegistryConfig(), Clock.SYSTEM) {

            @Override
            protected TimeUnit getBaseTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            protected void publish() {
                getMeters().stream()
                        .filter(m -> m.getId().getName().startsWith("message"))
                        .map(m -> get(m.getId().getName()).counter())
                        .forEach(c -> LOG.info("{} = {}", c.getId().getName(), val(c)));
            }

            @Override
            public void start(ThreadFactory threadFactory) {
                super.start(Executors.defaultThreadFactory());
            }
        };
        stepMeterRegistry.start(Executors.defaultThreadFactory());
        return stepMeterRegistry;
    }

    private StepRegistryConfig stepRegistryConfig() {
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

    private long val(Counter cnt) {
        return Math.round(cnt.count());
    }
}
