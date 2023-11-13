/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.IotDeviceId;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sdk.rest.model.Device;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceSynchronizationTaskTest {

    private static final String LO_DEVICES_GROUP = "device-group-lo";
    private static final int SYNCHRONIZATION_POOL_SIZE = 1;
    public static final List<Device> LO_DEVICES = Arrays.asList(
            new Device().withId("lo-device-id-01").withName("lo-device-name-01"),
            new Device().withId("lo-device-id-02").withName("lo-device-name-02")
    );
    public static final List<IotDeviceId> IOT_DEVICES = Collections.singletonList(
            new IotDeviceId("lo-device-id-03")
    );

    @Mock
    private IotHubAdapter iotHubAdapter;
    @Mock
    private LoAdapter loAdapter;
    private DeviceSynchronizationTask deviceSynchronizationTask;

    @BeforeEach
    void setUp() {
        when(loAdapter.getDevices(LO_DEVICES_GROUP)).thenReturn(LO_DEVICES);
        when(iotHubAdapter.getIotDeviceIds()).thenReturn(IOT_DEVICES);

        AzureIotHubProperties azureIotHubProperties = new AzureIotHubProperties();
        azureIotHubProperties.setLoDevicesGroup(LO_DEVICES_GROUP);
        azureIotHubProperties.setSynchronizationThreadPoolSize(SYNCHRONIZATION_POOL_SIZE);
        deviceSynchronizationTask = new DeviceSynchronizationTask(iotHubAdapter, loAdapter, azureIotHubProperties);
    }

    @Test
    void shouldSynchronizeDevicesBetweenLApiClientAndIotHubAdapter() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(deviceSynchronizationTask);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(loAdapter, times(1)).getDevices(LO_DEVICES_GROUP);
        verify(iotHubAdapter, times(2)).createOrGetIotDeviceClient(anyString());
        verify(iotHubAdapter, times(1)).getIotDeviceIds();
        verify(iotHubAdapter, times(1)).deleteDevice(anyString());
        verify(loAdapter, never()).startListeningForMessages();
    }
}