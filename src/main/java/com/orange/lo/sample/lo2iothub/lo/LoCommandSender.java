/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.lo.sample.lo2iothub.exceptions.CommandException;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.Commands;
import com.orange.lo.sdk.rest.devicemanagement.DeviceManagement;
import com.orange.lo.sdk.rest.model.CommandAddRequest;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

public class LoCommandSender {

    private final LOApiClient loApiClient;
    private final ObjectMapper objectMapper;
    private final RetryPolicy<Void> commandRetryPolicy;

    public LoCommandSender(LOApiClient loApiClient, ObjectMapper objectMapper, RetryPolicy<Void> commandRetryPolicy) {
        this.loApiClient = loApiClient;
        this.objectMapper = objectMapper;
        this.commandRetryPolicy = commandRetryPolicy;
    }

    public void send(String deviceId, String command) {
        Fallback<Void> objectFallback = Fallback.ofException(e -> new CommandException(e.getLastFailure()));
        Failsafe.with(objectFallback, commandRetryPolicy)
                .run(() -> {
                    CommandAddRequest commandAddRequest = objectMapper.readValue(command, CommandAddRequest.class);
                    System.out.println("//////////////////////// " + deviceId + " ////////// " + command + " ////////// " + commandAddRequest);
                    DeviceManagement deviceManagement = loApiClient.getDeviceManagement();
                    Commands commands = deviceManagement.getCommands();
                    commands.addCommand(deviceId, commandAddRequest);
                });
    }
}
