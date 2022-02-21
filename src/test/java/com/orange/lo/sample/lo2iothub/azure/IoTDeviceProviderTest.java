/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.service.Device;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwinDevice;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IoTDeviceProviderTest {

    private static final String DEVICE_ID = "iot-device-id";
    private static final String TAG_PLATFORM_KEY = "platform";
    private static final String TAG_PLATFORM_VALUE = "value";

    @Mock
    Device device;
    @Mock
    DeviceTwin deviceTwin;
    @Mock
    RegistryManager registryManager;
    @Mock
    DeviceTwinDevice deviceTwinDevice;
    private IoTDeviceProvider ioTDeviceProvider;

    @BeforeEach
    void setUp() {
        ioTDeviceProvider = new IoTDeviceProvider(deviceTwin, registryManager, TAG_PLATFORM_KEY, TAG_PLATFORM_VALUE);
    }

    @Test
    void shouldProperlyGetDevice() throws IOException, IotHubException {
        when(registryManager.getDevice(DEVICE_ID)).thenReturn(device);

        Device d = ioTDeviceProvider.getDevice(DEVICE_ID);
        assertEquals(device, d);
    }

    @Test
    void shouldProperlyGetDevices() throws IOException, IotHubException {
        when(deviceTwin.hasNextDeviceTwin(any())).thenReturn(true).thenReturn(false);
        when(deviceTwin.getNextDeviceTwin(any())).thenReturn(deviceTwinDevice);
        when(deviceTwinDevice.getDeviceId()).thenReturn(DEVICE_ID);

        ioTDeviceProvider.createDevice(DEVICE_ID);
        List<IoTDevice> devices = ioTDeviceProvider.getDevices();
        assertEquals(1, devices.size());
    }

    @Test
    void shouldProperlyDeleteDevice() {
        ioTDeviceProvider.createDevice(DEVICE_ID);
        ioTDeviceProvider.deleteDevice(DEVICE_ID);
        Device device = ioTDeviceProvider.getDevice(DEVICE_ID);
        assertNull(device);
    }

    @Test
    void shouldProperlyCreateDevice() {
        Device device = ioTDeviceProvider.createDevice(DEVICE_ID);
        assertEquals(DEVICE_ID, device.getDeviceId());
    }

}