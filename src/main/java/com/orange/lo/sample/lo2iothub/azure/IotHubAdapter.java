/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
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

    private IoTDeviceProvider ioTDeviceProvider;
    private MessageSender messageSender;
    private boolean deviceSynchronization;
    private DeviceClientManager deviceClientManager;

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, MessageSender messageSender,
            DeviceClientManager deviceClientManager, boolean deviceSynchronization) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.messageSender = messageSender;
        this.deviceClientManager = deviceClientManager;
        this.deviceSynchronization = deviceSynchronization;
    }

    public void sendMessage(String loClientId, String message) {
        try {
            DeviceClient deviceClient = createOrGetDeviceClient(loClientId);
            messageSender.sendMessage(message, deviceClient);
        } catch (Exception e) {
            LOG.error("Cannot send message", e);
        }
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                deviceClientManager.removeDeviceClient(deviceId);
                ioTDeviceProvider.deleteDevice(deviceId);
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                LOG.error("Cannot delete device " + deviceId, e);
            }
        } else {
            throw new DeviceSynchronizationException(deviceId);
        }
    }

    public DeviceClient createOrGetDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {

            if (!deviceClientManager.containsDeviceClient(deviceId)) {
                LOG.debug("Creating device client that will be multiplexed: {} ", deviceId);
                Device device = ioTDeviceProvider.getDevice(deviceId);
                // no device in iot hub
                if (device == null) {
                    if (deviceSynchronization) {
                        device = ioTDeviceProvider.createDevice(deviceId);
                    } else {
                        throw new DeviceSynchronizationException(deviceId);
                    }
                }
                try {
                    deviceClientManager.createDeviceClient(device);
                    LOG.info("Device client created for {}", deviceId);
                } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                    LOG.error("Device client creation for {} failed, because of {}", deviceId, e.getMessage());
                }
            }
            return deviceClientManager.getDeviceClient(deviceId);
        }
    }

    public List<IoTDevice> getDevices() {
        return ioTDeviceProvider.getDevices();
    }
}