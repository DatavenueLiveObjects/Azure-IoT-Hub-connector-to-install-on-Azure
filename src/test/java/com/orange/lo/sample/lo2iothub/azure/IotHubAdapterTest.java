/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IotHubAdapterTest {

    private static final String DEVICE_ID = "iot-device-id";
    public static final String CONNECTION_STRING = "HostName=azure-devices.net;DeviceId=iot-device-id;SharedAccessKey=b3Jhbmdl";

    @Mock
    private IoTDeviceProvider ioTDeviceProvider;
    @Mock
    private MessageSender messageSender;
    @Mock
    private IotClientCache iotClientCache;
    @Mock
    private AzureIotHubProperties iotHubProperties;
    @Mock
    private Device device;
    private DeviceClient deviceClient;
    private IotHubAdapter iotHubAdapter;

    @BeforeEach
    void setUp() throws URISyntaxException {
        iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, messageSender, iotClientCache, iotHubProperties, true);
        deviceClient = new DeviceClient(CONNECTION_STRING, IotHubClientProtocol.MQTT);
    }

    @Test
    void shouldCallMessageSenderWhenMessageIsSent() {
        when(iotClientCache.get(any())).thenReturn(deviceClient);
        String message = "{\"metadata\":{\"source\":\"iot-device-id\"}}";

        iotHubAdapter.sendMessage(message);

        verify(messageSender, times(1)).sendMessage(message, deviceClient);
    }

    @Test
    void shouldUseIotClientCacheAndIoTDeviceProviderWhenDeviceIsDeleted() {
        iotHubAdapter.deleteDevice(DEVICE_ID);

        verify(iotClientCache, times(1)).remove(DEVICE_ID);
        verify(ioTDeviceProvider, times(1)).deleteDevice(DEVICE_ID);
    }

    @Test
    void shouldUseIotClientCacheAndIoTDeviceProviderToCreateDeviceClient() {
        when(ioTDeviceProvider.getDevice(DEVICE_ID)).thenReturn(device);
        when(iotClientCache.get(DEVICE_ID)).thenReturn(deviceClient);

        DeviceClient dc = iotHubAdapter.createDeviceClient(DEVICE_ID);

        assertEquals(deviceClient, dc);
        verify(ioTDeviceProvider, times(1)).getDevice(DEVICE_ID);
        verify(iotClientCache, times(2)).get(DEVICE_ID);
    }

    @Test
    void shouldThrowDeviceSynchronizationExceptionDuringCreatingDeviceClientWhenSynchronizationIsDisabled() {
        IotHubAdapter iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, messageSender, iotClientCache, iotHubProperties, false);
        when(ioTDeviceProvider.getDevice(DEVICE_ID)).thenReturn(null);

        Assertions.assertThrows(DeviceSynchronizationException.class, () -> {
            iotHubAdapter.createDeviceClient(DEVICE_ID);            
        }, "DeviceSynchronizationException");
    }

    @Test
    void shouldUseIoTDeviceProviderWhenGetDevicesIsCalled() {
        ArrayList<IoTDevice> expectedDevices = new ArrayList<>();
        when(ioTDeviceProvider.getDevices()).thenReturn(expectedDevices);
        List<IoTDevice> devices = iotHubAdapter.getDevices();

        assertEquals(expectedDevices, devices);
        verify(ioTDeviceProvider, times(1)).getDevices();
    }
}