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

    private IoTDeviceProvider ioTDeviceProvider;
    private MessageSender messageSender;
    private boolean deviceSynchronization;
    private DeviceManager deviceClientManager;

    public IotHubAdapter(IoTDeviceProvider ioTDeviceProvider, MessageSender messageSender,
                         DeviceManager deviceClientManager, boolean deviceSynchronization) {
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.messageSender = messageSender;
        this.deviceClientManager = deviceClientManager;
        this.deviceSynchronization = deviceSynchronization;
    }

    public void sendMessage(String loClientId, String message) {
        IoTHubClient ioTHubClient = createOrGetDeviceClient(loClientId);
        LoMessageDetails loMessageDetails = new LoMessageDetails(message, ioTHubClient);
        messageSender.sendMessage(loMessageDetails);
    }

    public void deleteDevice(String deviceId) {
        if (deviceSynchronization) {
            try {
                synchronized (deviceId.intern()) {
                    deviceClientManager.removeDeviceClient(deviceId);
                    ioTDeviceProvider.deleteDevice(deviceId);
                }
            } catch (InterruptedException | IotHubClientException | TimeoutException e) {
                LOG.error("Cannot delete device " + deviceId, e);
            }
        } else {
            throw new DeviceSynchronizationException("Cannot delete device " + deviceId + " because device synchronization is disabled");
        }
    }

    public IoTHubClient createOrGetDeviceClient(String deviceId) {
        synchronized (deviceId.intern()) {
            if (!deviceClientManager.containsDeviceClient(deviceId)) {
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
                deviceClientManager.createDeviceClient(device);
                LOG.debug("Device client created for {}", deviceId);
            }
            return deviceClientManager.getDeviceClient(deviceId);
        }
    }

    public List<IoTDevice> getDevices() {
        return ioTDeviceProvider.getDevices();
    }
}