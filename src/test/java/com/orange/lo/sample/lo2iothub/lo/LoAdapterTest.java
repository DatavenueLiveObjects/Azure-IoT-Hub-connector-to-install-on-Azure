/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.DeviceManagement;
import com.orange.lo.sdk.rest.devicemanagement.GetDevicesFilter;
import com.orange.lo.sdk.rest.devicemanagement.GetGroupsFilter;
import com.orange.lo.sdk.rest.devicemanagement.Groups;
import com.orange.lo.sdk.rest.devicemanagement.Inventory;
import com.orange.lo.sdk.rest.model.Device;
import com.orange.lo.sdk.rest.model.Group;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoAdapterTest {
    private static final List<Device> DEVICES = Arrays.asList(
            new Device().withId("device-one").withName("device one"),
            new Device().withId("device-two").withName("device two"),
            new Device().withId("device-three").withName("device three"),
            new Device().withId("device-four").withName("device four"),
            new Device().withId("device-five").withName("device five")
    );
    private static final List<Group> GROUP_INSTANCES_FROM_LO = Arrays.asList(
            new Group().withId("root").withParentId(null).withPathNode(null),
            new Group().withId("aaTBvZUu").withParentId("root").withPathNode("device_group1"),
            new Group().withId("bbTBvZUu").withParentId("root").withPathNode("device_group2"),
            new Group().withId("ccTBvZUu").withParentId("bbTBvZUu").withPathNode("device_group2_sub"),
            new Group().withId("ddTBvZUu").withParentId("bbTBvZUu").withPathNode("device_group2_sub2"),
            new Group().withId("eeTBvZUu").withParentId("ddTBvZUu").withPathNode("device_group2_sub2_sub")
    );
    private static final int PAGE_SIZE = 20;

    @Mock
    private LOApiClient loApiClient;
    @Mock
    private DeviceManagement deviceManagement;
    @Mock
    private Groups groups;
    @Mock
    private Inventory inventory;
    @Mock
    private LiveObjectsProperties liveObjectsProperties;

    private LoAdapter loAdapter;

    @BeforeEach
    void setUp() {
        when(loApiClient.getDeviceManagement()).thenReturn(deviceManagement);
        when(deviceManagement.getInventory()).thenReturn(inventory);
        when(deviceManagement.getGroups()).thenReturn(groups);
        when(groups.getGroups(any(GetGroupsFilter.class))).thenReturn(GROUP_INSTANCES_FROM_LO);
        when(inventory.getDevices(any(GetDevicesFilter.class))).thenAnswer(LoAdapterTest::getObjects);
        when(liveObjectsProperties.getQos()).thenReturn(1);

        this.loAdapter = new LoAdapter(loApiClient, PAGE_SIZE, new RetryPolicy<>(), new RetryPolicy<>(), liveObjectsProperties.getQos());
    }

    @NotNull
    private static List<?> getObjects(InvocationOnMock invocation) {
        GetDevicesFilter argument = (GetDevicesFilter) invocation.getArguments()[0];
        switch (argument.getGroupId()) {
            case "aaTBvZUu":
                return Collections.singletonList(DEVICES.get(0));
            case "bbTBvZUu":
                return Collections.singletonList(DEVICES.get(1));
            case "ccTBvZUu":
                return Collections.singletonList(DEVICES.get(2));
            case "eeTBvZUu":
                return Collections.singletonList(DEVICES.get(3));
            case "root":
                return Collections.singletonList(DEVICES.get(4));
            default:
                return new ArrayList<>();
        }
    }

    @Test
    void shouldGetOneDeviceWhenGroupWithOneDeviceAndNoSubgroupsIsUsed() {
        List<Device> devices = loAdapter.getDevices("device_group2_sub2_sub");

        assertEquals(1, devices.size());
        Device device = devices.get(0);
        assertEquals("device-four", device.getId());

    }

    @Test
    void shouldGetWholeDevicesFromSelectedGroupAndSubgroupsWhenGroupWithSubgroupsIsUsed() {
        List<Device> devices = loAdapter.getDevices("device_group2");

        assertEquals(3, devices.size());

        List<String> expectedDeviceIds = Arrays.asList("device-two", "device-three", "device-four");
        List<String> actualDeviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        assertTrue(listEqualsIgnoreOrder(expectedDeviceIds, actualDeviceIds));
    }

    @Test
    void shouldGetWholeDevicesFromWholeGroupsWhenRootGroupIsUsed() {
        List<Device> devices = loAdapter.getDevices("root");

        assertEquals(5, devices.size());

        List<String> expectedDeviceIds = DEVICES.stream().map(Device::getId).collect(Collectors.toList());
        List<String> actualDeviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        assertTrue(listEqualsIgnoreOrder(expectedDeviceIds, actualDeviceIds));
    }

    @Test
    void shouldGetWholeDevicesFromWholeGroupsWhenNonExistentGroupIsUsed2() {
        List<Device> devices = loAdapter.getDevices("NonExistentGroup");

        assertEquals(5, devices.size());

        List<String> expectedDeviceIds = DEVICES.stream().map(Device::getId).collect(Collectors.toList());
        List<String> actualDeviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        assertTrue(listEqualsIgnoreOrder(expectedDeviceIds, actualDeviceIds));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void shouldAddMonitoringPartForProdEnvironment(String group, int numberOfGroupApiCalls, int numberOfInventoryApiCalls) {
        loAdapter.getDevices(group);

        verify(groups, times(numberOfGroupApiCalls)).getGroups(any(GetGroupsFilter.class));
        verify(inventory, times(numberOfInventoryApiCalls)).getDevices(any(GetDevicesFilter.class));
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("root", 1, 6),
                Arguments.of("device_group2_sub2_sub", 1, 1),
                Arguments.of("device_group2", 1, 4)
        );
    }

    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

}