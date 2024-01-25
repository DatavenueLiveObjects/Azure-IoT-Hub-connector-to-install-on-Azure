/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IotHubAdapterTest {

    private static final String DEVICE_ID = "iot-device-id";
    private static final int MESSAGE_ID = 1;
    public static final String CONNECTION_STRING = "HostName=azure-devices.net;DeviceId=iot-device-id;SharedAccessKey=b3Jhbmdl";

    @Mock
    private IoTDeviceProvider ioTDeviceProvider;
    @Mock
    private AzureIotHubProperties iotHubProperties;
    @Mock
    private Device device;
    @Mock
    private DevicesManager devicesManager;
    @Mock
    private DeviceClientManager deviceClientManager;

    private IotHubAdapter iotHubAdapter;
    private MultiplexingClientManager multiplexingClientManager;


    @BeforeEach
    void setUp() throws URISyntaxException {
        iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, devicesManager, true, null);
    }

    @Test
    void shouldCallMessageSenderWhenMessageIsSent() {

        when(devicesManager.containsDeviceClient(DEVICE_ID)).thenReturn(true);
        when(devicesManager.getDeviceClientManager(DEVICE_ID)).thenReturn(deviceClientManager);

        String message = "{\"metadata\":{\"source\":\"iot-device-id\"}}";

        iotHubAdapter.sendMessage(DEVICE_ID, MESSAGE_ID, message);

        verify(deviceClientManager, times(1)).sendMessage(eq(MESSAGE_ID), any());
    }

    @Test
    void shouldUseDevicesManagerAndIoTDeviceProviderWhenDeviceIsDeleted()
            throws InterruptedException, IotHubClientException, TimeoutException {

        iotHubAdapter.deleteDevice(DEVICE_ID);

        verify(devicesManager, times(1)).removeDeviceClient(DEVICE_ID);
        verify(ioTDeviceProvider, times(1)).deleteDevice(DEVICE_ID);
    }

    @Test
    void shouldCreateDeviceClientManager() {

        when(devicesManager.containsDeviceClient(DEVICE_ID)).thenReturn(false);
        when(ioTDeviceProvider.getDevice(DEVICE_ID)).thenReturn(device);

        iotHubAdapter.createOrGetDeviceClientManager(DEVICE_ID);

        verify(devicesManager, times(1)).containsDeviceClient(DEVICE_ID);
        verify(devicesManager, times(1)).createDeviceClient(device);
    }

    @Test
    void shouldGetExistingDeviceClientManager() {

            when(devicesManager.containsDeviceClient(DEVICE_ID)).thenReturn(true);
            when(devicesManager.getDeviceClientManager(DEVICE_ID)).thenReturn(deviceClientManager);

            iotHubAdapter.createOrGetDeviceClientManager(DEVICE_ID);

            verify(devicesManager, times(1)).containsDeviceClient(DEVICE_ID);
            verify(devicesManager, times(0)).createDeviceClient(device);
            verify(devicesManager, times(1)).getDeviceClientManager(DEVICE_ID);
    }

    @Test
    void shouldThrowDeviceSynchronizationExceptionDuringCreatingDeviceClientWhenSynchronizationIsDisabled() {
        IotHubAdapter iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, devicesManager, false, null);

        when(ioTDeviceProvider.getDevice(DEVICE_ID)).thenReturn(null);

        assertThrows(DeviceSynchronizationException.class, () -> {
            iotHubAdapter.createOrGetDeviceClientManager(DEVICE_ID);
        }, "DeviceSynchronizationException");
    }

    @Test
    void shouldUseIoTDeviceProviderWithSynchronization() {
            iotHubAdapter.getIotDeviceIds();
            verify(ioTDeviceProvider, times(1)).getDevices(true);
        }
}