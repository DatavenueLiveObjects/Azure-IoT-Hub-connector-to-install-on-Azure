/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubNotFoundException;
import com.microsoft.azure.sdk.iot.service.query.TwinQueryResponse;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.microsoft.azure.sdk.iot.service.registry.RegistryClient;
import com.microsoft.azure.sdk.iot.service.registry.RegistryStatistics;
import com.microsoft.azure.sdk.iot.service.twin.Twin;
import com.microsoft.azure.sdk.iot.service.twin.TwinClient;
import com.orange.lo.sample.lo2iothub.exceptions.IotDeviceProviderException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoTDeviceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String tagPlatformKey;
    private String tagPlatformValue;
    private TwinClient twinClient;
    private RegistryClient registryClient;

    public IoTDeviceProvider(TwinClient twinClient, RegistryClient registryClient, String tagPlatformKey,
                             String tagPlatformValue) {
        this.twinClient = twinClient;
        this.registryClient = registryClient;
        this.tagPlatformKey = tagPlatformKey;
        this.tagPlatformValue = tagPlatformValue;
    }

    public Device getDevice(String deviceId) {
        try {
            return registryClient.getDevice(deviceId);
        } catch (IotHubNotFoundException e) {
            return null;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while retrieving device: " + deviceId, e);
        }
    }

    public boolean deviceExists(String deviceId) {
        try {
            registryClient.getDevice(deviceId);
            return true;
        } catch (IotHubNotFoundException e) {
            return false;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while retrieving device: " + deviceId, e);
        }
    }

    public List<IotDeviceId> getDevices(boolean queryByTags) {
        List<IotDeviceId> list = new ArrayList<>();
        try {
            String selectingQuery = "SELECT * FROM devices";
            if(queryByTags) {
                selectingQuery += " WHERE tags." + tagPlatformKey + "='" + tagPlatformValue + "'";
            }
            TwinQueryResponse query = twinClient
                    .query(selectingQuery);
            while (query.hasNext()) {
                list.add(new IotDeviceId(query.next().getDeviceId()));
            }
            return list;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while retrieving devices", e);
        }
    }

    public void deleteDevice(String deviceId) {
        try {
            registryClient.removeDevice(deviceId);
        } catch (IotHubException | IOException e) {
            LOG.error("Cannot remove device {}", deviceId);
        }
    }

    public Device createDevice(String deviceId) {
        try {
            Device device = new Device(deviceId);
            registryClient.addDevice(device);
            setPlatformTag(deviceId);
            return device;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while creating device", e);
        }
    }

    private void setPlatformTag(String deviceId) {
        try {
            Twin twin = new Twin(deviceId);
            twin.getTags().put(tagPlatformKey, tagPlatformValue);
            twinClient.patch(twin);
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while creating device", e);
        }
    }

    public void logRegistryClientStatistics() {
        try {
            RegistryStatistics statistics = registryClient.getStatistics();
            LOG.info("Total device count: {}", statistics.getTotalDeviceCount());
            LOG.info("Enabled device count: {}", statistics.getEnabledDeviceCount());
            LOG.info("Disabled device count: {}", statistics.getDisabledDeviceCount());
        } catch (Exception e) {
            throw new IotDeviceProviderException("Error while getting RegistryClient statistics", e);
        }
    }
}