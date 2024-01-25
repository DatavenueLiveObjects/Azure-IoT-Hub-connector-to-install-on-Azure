/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.orange.lo.sample.lo2iothub.exceptions.CommandException;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.Commands;
import com.orange.lo.sdk.rest.devicemanagement.DeviceManagement;
import com.orange.lo.sdk.rest.model.CommandAddRequest;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

public class LoCommandSender {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMapper objectMapper;
    private final RetryPolicy<Void> commandRetryPolicy;
    private final LoAdapter loAdapter;

    public LoCommandSender(LoAdapter loAdapter, ObjectMapper objectMapper, RetryPolicy<Void> commandRetryPolicy) {
        this.loAdapter = loAdapter;
        this.objectMapper = objectMapper;
        this.commandRetryPolicy = commandRetryPolicy;
    }

    public IotHubMessageResult send(String deviceId, String command) {
        try {
            Fallback<Void> objectFallback = Fallback.ofException(e -> new CommandException(e.getLastFailure()));
            Failsafe.with(objectFallback, commandRetryPolicy).run(() -> {
                CommandAddRequest commandAddRequest = objectMapper.readValue(command, CommandAddRequest.class);
                loAdapter.sendCommand(deviceId, commandAddRequest);
            });
            return IotHubMessageResult.COMPLETE;
        } catch (CommandException e) {
            LOG.error("Cannot send command", e);
            return IotHubMessageResult.REJECT;
        }
    }
}
