/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.service.Device;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwinDevice;
import com.microsoft.azure.sdk.iot.service.devicetwin.Pair;
import com.microsoft.azure.sdk.iot.service.devicetwin.Query;
import com.microsoft.azure.sdk.iot.service.devicetwin.SqlQuery;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubNotFoundException;
import com.orange.lo.sample.lo2iothub.exceptions.IotDeviceProviderException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoTDeviceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DeviceTwin deviceTwin;
    private RegistryManager registryManager;
    private String tagPlatformKey;
    private String tagPlatformValue;

    public IoTDeviceProvider(DeviceTwin deviceTwin, RegistryManager registryManager, String tagPlatformKey,
                             String tagPlatformValue) {
        this.deviceTwin = deviceTwin;
        this.registryManager = registryManager;
        this.tagPlatformKey = tagPlatformKey;
        this.tagPlatformValue = tagPlatformValue;
    }

    public Device getDevice(String deviceId) {
        try {
            return registryManager.getDevice(deviceId);
        } catch (IotHubNotFoundException e) {
            return null;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while retrieving device ", e);
        }
    }

    public List<IoTDevice> getDevices() {
        List<IoTDevice> list = new ArrayList<>();
        try {
            String where = "tags." + tagPlatformKey + "=" + "'" + tagPlatformValue + "'";
            SqlQuery sqlQuery = SqlQuery.createSqlQuery("*", SqlQuery.FromType.DEVICES, where, null);
            Query queryTwin = deviceTwin.queryTwin(sqlQuery.getQuery());
            while (deviceTwin.hasNextDeviceTwin(queryTwin)) {
                DeviceTwinDevice deviceTwinDevice = deviceTwin.getNextDeviceTwin(queryTwin);
                list.add(new IoTDevice(deviceTwinDevice.getDeviceId()));
            }
            return list;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while retrieving devices", e);
        }
    }

    public void deleteDevice(String deviceId) {
        try {
            registryManager.removeDevice(deviceId);
        } catch (IotHubException | IOException e) {
            LOG.error("Cannot remove device {}", deviceId);
        }
    }

    public Device createDevice(String deviceId) {
        try {
            Device device = Device.createFromId(deviceId, null, null);
            registryManager.addDevice(device);
            setPlatformTag(deviceId);
            return device;
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while creating device", e);
        }
    }

    private void setPlatformTag(String deviceId) {
        try {
            Set<Pair> tags = new HashSet<>();
            tags.add(new Pair(tagPlatformKey, tagPlatformValue));
            DeviceTwinDevice deviceTwinDevice = new DeviceTwinDevice(deviceId);
            deviceTwinDevice.setTags(tags);
            deviceTwin.updateTwin(deviceTwinDevice);
        } catch (IotHubException | IOException e) {
            throw new IotDeviceProviderException("Error while creating device", e);
        }
    }
}