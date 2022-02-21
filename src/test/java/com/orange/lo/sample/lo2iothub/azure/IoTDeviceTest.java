/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

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