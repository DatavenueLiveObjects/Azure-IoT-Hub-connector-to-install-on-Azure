package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class IotClientCacheTest {

    private static final String DEVICE_ID = "iot-device-id";
    public static final String CONNECTION_STRING = "HostName=azure-devices.net;DeviceId=iot-device-id;SharedAccessKey=b3Jhbmdl";

    private DeviceClient deviceClient;
    private IotClientCache iotClientCache;

    @BeforeEach
    void setUp() throws URISyntaxException {
        iotClientCache = new IotClientCache();
        deviceClient = new DeviceClient(CONNECTION_STRING, IotHubClientProtocol.MQTT);
    }

    @Test
    void shouldProperlyAddDeviceClient() {
        iotClientCache.add(DEVICE_ID, deviceClient);
        DeviceClient client = iotClientCache.get(DEVICE_ID);
        assertEquals(deviceClient, client);
    }

    @Test
    void shouldProperlyGetDeviceClient() {
        iotClientCache.add(DEVICE_ID, deviceClient);
        DeviceClient client = iotClientCache.get(DEVICE_ID);
        assertEquals(deviceClient, client);
    }

    @Test
    void shouldProperlyRemoveDeviceClient() {
        iotClientCache.add(DEVICE_ID, deviceClient);
        boolean remove = iotClientCache.remove(DEVICE_ID);
        assertTrue(remove);
        assertNull(iotClientCache.get(DEVICE_ID));
    }

    @Test
    void shouldProperlyGetDeviceIds() {
        iotClientCache.add(DEVICE_ID, deviceClient);
        Set<String> deviceIds = iotClientCache.getDeviceIds();
        assertTrue(deviceIds.contains(DEVICE_ID));
    }
}