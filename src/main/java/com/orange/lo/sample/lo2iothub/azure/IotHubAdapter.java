/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.exceptions.DeviceSynchronizationException;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotHubAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IoTDeviceProvider ioTDeviceProvider;
    private final boolean deviceSynchronization;
    private final DevicesManager deviceManager;

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, DevicesManager deviceClientManager, boolean deviceSynchronization) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.deviceManager = deviceClientManager;
        this.deviceSynchronization = deviceSynchronization;
    }

    public void sendMessage(String loClientId, String message) {
        DeviceClientManager ioTHubClient = createOrGetIotDeviceClient(loClientId);
        ioTHubClient.sendMessage(message);
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                synchronized (deviceId.intern()) {
                    deviceManager.removeDeviceClient(deviceId);
                    ioTDeviceProvider.deleteDevice(deviceId);
                }
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                LOG.error("Cannot delete device " + deviceId, e);
            }
        } else {
            throw new DeviceSynchronizationException("Cannot delete device " + deviceId + " because device synchronization is disabled");
        }
    }

    public DeviceClientManager createOrGetIotDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {
            if (!deviceManager.containsDeviceClient(deviceId)) {
                LOG.debug("Creating device client that will be multiplexed: {} ", deviceId);
                Device device = ioTDeviceProvider.getDevice(deviceId);
                // no device in iot hub
                if (device == null) {
                    if (deviceSynchronization) {
                        device = ioTDeviceProvider.createDevice(deviceId);
                    } else {
                        throw new DeviceSynchronizationException("Device " + deviceId + " does not exist in IoT Hub");
                    }
                }
                deviceManager.createDeviceClient(device);
                LOG.debug("Device client created for {}", deviceId);
            }
            return deviceManager.getDeviceClient(deviceId);
        }
    }

    public List<IotDeviceId> getIotDeviceIds() {
        return ioTDeviceProvider.getDevices();
    }
}