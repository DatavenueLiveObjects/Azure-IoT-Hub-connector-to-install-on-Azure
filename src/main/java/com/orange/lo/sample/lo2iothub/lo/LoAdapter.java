/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.fifomqtt.DataManagementFifo;
import com.orange.lo.sdk.rest.devicemanagement.GetDevicesFilter;
import com.orange.lo.sdk.rest.devicemanagement.GetGroupsFilter;
import com.orange.lo.sdk.rest.devicemanagement.Groups;
import com.orange.lo.sdk.rest.devicemanagement.Inventory;
import com.orange.lo.sdk.rest.model.Device;
import com.orange.lo.sdk.rest.model.Group;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_GROUP_ID = "root";

    private final LOApiClient loApiClient;
    private final Map<String, String> groupsMap;
    private final int pageSize;
    private final RetryPolicy<List<Group>> groupRetryPolicy;
    private final RetryPolicy<List<Device>> deviceRetryPolicy;

    public LoAdapter(LOApiClient loApiClient, int pageSize, RetryPolicy<List<Group>> groupRetryPolicy,
                     RetryPolicy<List<Device>> deviceRetryPolicy) {
        this.loApiClient = loApiClient;
        this.groupsMap = new HashMap<>();
        this.pageSize = pageSize;
        this.groupRetryPolicy = groupRetryPolicy;
        this.deviceRetryPolicy = deviceRetryPolicy;
        initialize();
    }

    private void initialize() {
        LOG.info("Managing groups of devices");

        try {
            getExistingGroups();
        } catch (Exception e) {
            LOG.error("Unexpected error while managing group: {}", e.getMessage());
            System.exit(1);
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
                loGroups.forEach(g -> groupsMap.put(g.getPathNode(), g.getId()));
                if (loGroups.size() < pageSize) {
                    break;
                }
            } catch (HttpClientErrorException e) {
                LOG.error("Cannot retrieve information about groups \n {}", e.getResponseBodyAsString());
                System.exit(1);
            }
        }
    }

    public List<Device> getDevices(String groupName) {
        List<Device> devices = new ArrayList<>(pageSize);
        Inventory inventory = loApiClient.getDeviceManagement().getInventory();
        String groupsMapOrDefault = groupsMap.getOrDefault(groupName, DEFAULT_GROUP_ID);

        for (int offset = 0; ; offset++) {
            GetDevicesFilter devicesFilter = new GetDevicesFilter()
                    .withGroupId(groupsMapOrDefault)
                    .withLimit(pageSize)
                    .withOffset(offset * pageSize);
            List<Device> loDevices = Failsafe.with(deviceRetryPolicy)
                    .get(() -> inventory.getDevices(devicesFilter));
            LOG.trace("Got {} devices", loDevices.size());
            devices.addAll(loDevices);
            if (loDevices.size() < pageSize) {
                break;
            }
        }
        LOG.trace("Devices: {}", devices);
        return devices;
    }

    public void startListeningForMessages() {
        LOG.info("Starting listening for messages...");
        DataManagementFifo dataManagementFifo = loApiClient.getDataManagementFifo();
        dataManagementFifo.connectAndSubscribe();
    }
}
