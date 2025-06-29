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
import com.orange.lo.sample.lo2iothub.utils.Counters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class IotHubAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IoTDeviceProvider ioTDeviceProvider;
    private final boolean deviceSynchronization;
    private final DevicesManager devicesManager;
    private final Counters counters;

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, DevicesManager deviceClientManager, boolean deviceSynchronization, Counters counters) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.devicesManager = deviceClientManager;
        this.deviceSynchronization = deviceSynchronization;
        this.counters = counters;
    }

    public void sendMessage(String loClientId, int loMessageId, String message) {
        DeviceClientManager deviceClientManager = createOrGetDeviceClientManager(loClientId);
        deviceClientManager.sendMessage(loMessageId, message);
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                synchronized (deviceId.intern()) {
                    devicesManager.removeDeviceClient(deviceId);
                    ioTDeviceProvider.deleteDevice(deviceId);
                }
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                LOG.error("Cannot delete device " + deviceId, e);
            }
        } else {
            throw new DeviceSynchronizationException("Cannot delete device " + deviceId + " because device synchronization is disabled");
        }
    }

    public DeviceClientManager createOrGetDeviceClientManager(String deviceId) {
        synchronized (deviceId.intern()) {
            if (!devicesManager.containsDeviceClient(deviceId)) {
                LOG.debug("Creating device client that will be multiplexed: {} ", deviceId);
                Device device = null;
                try {
                    device = ioTDeviceProvider.getDevice(deviceId);
                } catch (Exception e) {
                    LOG.debug("Problem with connection. Check IoT Hub credentials ", e);
                    counters.setCloudConnectionStatus(false);
                }

                // no device in iot hub
                if (device == null) {
                    if (deviceSynchronization) {
                        device = ioTDeviceProvider.createDevice(deviceId);
                    } else {
                        throw new DeviceSynchronizationException("Device " + deviceId + " does not exist in IoT Hub");
                    }
                }
                devicesManager.createDeviceClient(device);
                LOG.debug("Device client created for {}", deviceId);
            }
            return devicesManager.getDeviceClientManager(deviceId);
        }
    }

    public List<IotDeviceId> getIotDeviceIds() {
        return ioTDeviceProvider.getDevices(deviceSynchronization);
    }

    public void removeDeviceClientsForNonExistentDevices(Set<String> existingDeviceIDs) {
        devicesManager.keepDeviceClientsOnlyForTheseDevices(existingDeviceIDs);
    }

    public void logDeviceRegistryStatistics() {
        ioTDeviceProvider.logRegistryClientStatistics();
    }
}