package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.rest.devicemanagement.DeviceManagement;
import com.orange.lo.sdk.rest.devicemanagement.GetDevicesFilter;
import com.orange.lo.sdk.rest.devicemanagement.GetGroupsFilter;
import com.orange.lo.sdk.rest.devicemanagement.Groups;
import com.orange.lo.sdk.rest.devicemanagement.Inventory;
import com.orange.lo.sdk.rest.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoAdapterTest {

    private static final String GROUP_NAME = "root";
    private static final List<Device> DEVICES = Arrays.asList(
            new Device().withId("device-one").withName("device one"),
            new Device().withId("device-two").withName("device two")
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
    private LoAdapter loAdapter;

    @BeforeEach
    void setUp() {
        when(loApiClient.getDeviceManagement()).thenReturn(deviceManagement);
        when(deviceManagement.getInventory()).thenReturn(inventory);
        when(deviceManagement.getGroups()).thenReturn(groups);
        when(groups.getGroups(any(GetGroupsFilter.class))).thenReturn(new ArrayList<>());
        when(inventory.getDevices(any(GetDevicesFilter.class))).thenReturn(DEVICES);

        this.loAdapter = new LoAdapter(loApiClient, PAGE_SIZE);
    }

    @Test
    void shouldCorrectlyCallRestTemplateWhenGettingDevicesData() {
        List<Device> devices = loAdapter.getDevices(GROUP_NAME);

        assertEquals(2, devices.size());
        verify(groups, times(1)).getGroups(any(GetGroupsFilter.class));
        verify(inventory, times(1)).getDevices(any(GetDevicesFilter.class));
    }
}