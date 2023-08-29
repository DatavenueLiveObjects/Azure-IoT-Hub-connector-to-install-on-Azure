/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.orange.lo.sample.lo2iothub.utils.Counters;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Counter;
import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private Counters counterProvider;
    @Mock
    private Counter counter;
    @Mock
    private DeviceClient deviceClient;
    
    RetryPolicy<Void> messageRetryPolicy;

    private MessageSender messageSender;

    @BeforeEach
    void setUp() {
        messageRetryPolicy = new RetryPolicy<Void>().handleIf(e -> e instanceof IllegalStateException)
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .withMaxDuration(Duration.ofHours(1));
        messageSender = new MessageSender(counterProvider, 1_000L);
        messageSender.setMessageRetryPolicy(messageRetryPolicy);
    }

    @Test
    void sendMessage() {
        when(counterProvider.getMesasageSentAttemptCounter()).thenReturn(counter);
        String message = "{\"metadata\":{\"source\":\"iot-device-id\"}}";
        LoMessageDetails loMessageDetails = new LoMessageDetails(message, deviceClient);
        messageSender.sendMessage(loMessageDetails);
        verify(counterProvider, times(1)).getMesasageSentAttemptCounter();
    }
}