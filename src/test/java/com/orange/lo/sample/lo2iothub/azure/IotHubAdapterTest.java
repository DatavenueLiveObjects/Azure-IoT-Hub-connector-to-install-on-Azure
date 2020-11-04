package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.service.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IotHubAdapterTest {

    private static final String DEVICE_ID = "iot-device-id";
    public static final String CONNECTION_STRING = "HostName=azure-devices.net;DeviceId=iot-device-id;SharedAccessKey=b3Jhbmdl";

    @Mock
    private IoTDeviceProvider ioTDeviceProvider;
    @Mock
    private LoCommandSender loCommandSender;
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
        iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, loCommandSender, messageSender, iotClientCache, iotHubProperties);
        deviceClient = new DeviceClient(CONNECTION_STRING, IotHubClientProtocol.MQTT);
    }

    @Test
    void shouldCallMessageSenderWhenMessageIsSent() {
        when(iotClientCache.get(any())).thenReturn(deviceClient);
        Message<String> message = new GenericMessage<>("{\"metadata\":{\"source\":\"iot-device-id\"}}");

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
    void shouldUseIoTDeviceProviderWhenGetDevicesIsCalled() {
        ArrayList<IoTDevice> expectedDevices = new ArrayList<>();
        when(ioTDeviceProvider.getDevices()).thenReturn(expectedDevices);
        List<IoTDevice> devices = iotHubAdapter.getDevices();

        assertEquals(expectedDevices, devices);
        verify(ioTDeviceProvider, times(1)).getDevices();
    }
}