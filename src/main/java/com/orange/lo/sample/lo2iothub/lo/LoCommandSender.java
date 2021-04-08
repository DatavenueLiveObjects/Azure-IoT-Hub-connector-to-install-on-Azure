/**
* Copyright (c) Orange. All Rights Reserved.
*
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
import com.orange.lo.sdk.rest.model.CommandRequest;
import com.orange.lo.sdk.rest.model.CommandRequestValue;

public class LoCommandSender {

    private final LOApiClient loApiClient;
    private final ObjectMapper objectMapper;

    public LoCommandSender(LOApiClient loApiClient, ObjectMapper objectMapper) {
        this.loApiClient = loApiClient;
        this.objectMapper = objectMapper;
    }

    public void send(String deviceId, String command) {
        try {
            CommandRequestValue commandRequestValue = objectMapper.readValue(command, CommandRequestValue.class);
            CommandRequest commandRequest = new CommandRequest().value(commandRequestValue);
            CommandAddRequest commandAddRequest = new CommandAddRequest().withRequest(commandRequest);

            DeviceManagement deviceManagement = loApiClient.getDeviceManagement();
            Commands commands = deviceManagement.getCommands();
            commands.addCommand(deviceId, commandAddRequest);
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }
}
