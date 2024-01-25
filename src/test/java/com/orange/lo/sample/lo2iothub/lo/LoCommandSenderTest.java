/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.Commands;
import com.orange.lo.sdk.rest.devicemanagement.DeviceManagement;
import com.orange.lo.sdk.rest.model.CommandAddRequest;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoCommandSenderTest {

    @Mock
    private LoAdapter loAdapter;
    @Mock
    private DeviceManagement deviceManagement;
    @Mock
    private Commands commands;

    private LoCommandSender loCommandSender;

    @BeforeEach
    void setUp() {
        this.loCommandSender = new LoCommandSender(loAdapter, new ObjectMapper(), new RetryPolicy<>());
    }

    @Test
    void shouldSendCommandToCommandsEndpoint() {

        loCommandSender.send("device-Id", "{}");
        verify(loAdapter, times(1)).sendCommand(eq("device-Id"), any(CommandAddRequest.class));
    }
}