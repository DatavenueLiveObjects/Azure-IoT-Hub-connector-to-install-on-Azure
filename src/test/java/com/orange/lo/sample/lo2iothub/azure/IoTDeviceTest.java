package com.orange.lo.sample.lo2iothub.azure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IoTDeviceTest {

    private static final String DEVICE_ID = "iot-device-id";

    @Test
    void shouldProperlyReturnIoTDeviceId() {
        IoTDevice ioTDevice = new IoTDevice(DEVICE_ID);
        assertEquals(DEVICE_ID, ioTDevice.getId());
    }
}