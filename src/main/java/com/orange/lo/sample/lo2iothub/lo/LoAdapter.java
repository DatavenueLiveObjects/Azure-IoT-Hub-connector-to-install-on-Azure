/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.*;
import com.orange.lo.sdk.rest.model.CommandAddRequest;
import com.orange.lo.sdk.rest.model.CommandResponse;
import com.orange.lo.sdk.rest.model.Device;
import com.orange.lo.sdk.rest.model.Group;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_GROUP_ID = "root";

    private final LOApiClient loApiClient;
    private final int qos;
    private final Map<String, Group> groupsMap;
    private final int pageSize;
    private final RetryPolicy<List<Group>> groupRetryPolicy;
    private final RetryPolicy<List<Device>> deviceRetryPolicy;

    public LoAdapter(LOApiClient loApiClient, int pageSize, RetryPolicy<List<Group>> groupRetryPolicy,
                     RetryPolicy<List<Device>> deviceRetryPolicy, int qos) {
        this.loApiClient = loApiClient;
        this.qos = qos;
        this.groupsMap = new HashMap<>();
        this.pageSize = pageSize;
        this.groupRetryPolicy = groupRetryPolicy;
        this.deviceRetryPolicy = deviceRetryPolicy;
        try {
            initialize();
        } catch (Exception e) {
            throw new DeviceSynchronizationException("Problem with connection. Check iot hub and LO credentials");
        }
    }

    public void connect() {
        LOG.info("Connecting to LO...");
        loApiClient.getDataManagementFifo().connect();
    }

    public void startListeningForMessages() {
        LOG.info("Starting listening for messages...");
        loApiClient.getDataManagementFifo().connectAndSubscribe();
    }

    public void sendMessageAck(int messageId) {
        LOG.debug("Sending ack for message with id: {}", messageId);
        loApiClient.getDataManagementFifo().sendAck(messageId, qos);
    }

    public CommandResponse sendCommand(String deviceId, CommandAddRequest commandAddRequest) {
        LOG.debug("Sending command {} to device {}", commandAddRequest, deviceId);
        return loApiClient.getDeviceManagement().getCommands().addCommand(deviceId, commandAddRequest);
    }

    public List<Device> getDevices(String groupName) {
        List<Device> devices = new ArrayList<>(pageSize);
        Inventory inventory = loApiClient.getDeviceManagement().getInventory();

        List<String> deviceGroups = getGroupWithSubgroups(groupName);

        deviceGroups.forEach(groupId -> {
            List<Device> partDevices = new ArrayList<>();
            GetDevicesFilter devicesFilter = new GetDevicesFilter().withGroupId(groupId).withLimit(pageSize);

            while (true) {
                if (partDevices.size() > 0) {
                    devicesFilter.withBookmarkId(partDevices.get(partDevices.size() - 1).getId());
                }
                List<Device> loDevices = Failsafe.with(deviceRetryPolicy)
                        .get(() -> inventory.getDevices(devicesFilter));
                LOG.trace("Got {} devices", loDevices.size());
                partDevices.addAll(loDevices);
                if (loDevices.size() < pageSize) {
                    devices.addAll(partDevices);
                    break;
                }
            }
        });
        LOG.trace("Devices: {}", devices.size());
        return devices;
    }

    private void initialize() {
        LOG.info("Managing groups of devices");

        try {
            getExistingGroups();
        } catch (Exception e) {
            throw new DeviceSynchronizationException("Problem with connection. Check iot hub and LO credentials");
        }
    }

    private void getExistingGroups() {
        LOG.debug("Trying to get existing groups");
        Groups groups = loApiClient.getDeviceManagement().getGroups();
        for (int offset = 0; ; offset++) {
            try {
                GetGroupsFilter groupsFilter = new GetGroupsFilter().withLimit(pageSize)
                        .withOffset(offset * pageSize);
                List<Group> loGroups = Failsafe.with(groupRetryPolicy)
                        .get(() -> groups.getGroups(groupsFilter));
                loGroups.forEach(g -> groupsMap.put(g.getPathNode(), g));
                if (loGroups.size() < pageSize) {
                    break;
                }
            } catch (HttpClientErrorException e) {
                throw new DeviceSynchronizationException("Problem with connection. Check iot hub and LO credentials");
            }
        }
    }

    private List<String> getGroupWithSubgroups(String groupName) {
        String mainGroupId = getMainDeviceGroupId(groupName);
        List<String> s = getSubgroupsIds(mainGroupId, groupsMap);

        return Stream.concat(Stream.of(mainGroupId), s.stream()).collect(Collectors.toList());
    }

    private String getMainDeviceGroupId(String groupName) {
        Optional<Group> group = Optional.ofNullable(groupsMap.get(groupName));
        if(!group.isPresent()) {
            LOG.info("Group {} not found, all devices will be synchronized.", groupName);
        }
        return group.map(Group::getId).orElse(DEFAULT_GROUP_ID);
    }

    private List<String> getSubgroupsIds(String parentId, Map<String, Group> groupsMap) {
        List<String> groupsIds = new ArrayList<>();
        List<Group> values = new ArrayList<>(groupsMap.values());
        for (Group currentGroup : values) {
            String parentIdOfCurrentGroup = currentGroup.getParentId();
            if (parentIdOfCurrentGroup != null && parentIdOfCurrentGroup.equalsIgnoreCase(parentId)) {
                String currentGroupId = currentGroup.getId();
                groupsIds.add(currentGroupId);
                List<String> subgroupsOfGivenGroup = getSubgroupsIds(currentGroupId, groupsMap);
                groupsIds.addAll(subgroupsOfGivenGroup);
            }
        }
        return groupsIds;
    }
}
