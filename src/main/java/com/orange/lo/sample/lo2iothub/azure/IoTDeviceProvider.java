/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.azure.sdk.iot.service.devicetwin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.sdk.iot.service.Device;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubNotFoundException;

@Component
public class IoTDeviceProvider {

	private static final Logger LOG = LoggerFactory.getLogger(IoTDeviceProvider.class);
		
	private DeviceTwin deviceTwin;
	private RegistryManager registryManager;
	private AzureProperties azureProperties;
	
	@Autowired
	public IoTDeviceProvider(DeviceTwin deviceTwin, RegistryManager registryManager, AzureProperties azureProperties) {
		this.deviceTwin = deviceTwin;
		this.registryManager = registryManager;
		this.azureProperties = azureProperties;
	}
	
	public Device getDevice(String deviceId) {
		try {
			return registryManager.getDevice(deviceId);
		} catch (IotHubNotFoundException e) {
			return null;
		} catch (JsonSyntaxException | IOException | IotHubException e) {
			LOG.error("Error while retrieving device ", e);
			return null;
		}
	}
	
	public List<IoTDevice> getDevices() {
		List<IoTDevice> list = Lists.newArrayList();
		try {
			String where = "tags." + azureProperties.getTagPlatformKey() + "=" + "'" + azureProperties.getTagPlatformValue() + "'";
			SqlQuery sqlQuery = SqlQuery.createSqlQuery("*", SqlQuery.FromType.DEVICES, where, null);
			Query queryTwin = deviceTwin.queryTwin(sqlQuery.getQuery());
			while (deviceTwin.hasNextDeviceTwin(queryTwin)) {
				DeviceTwinDevice deviceTwinDevice = deviceTwin.getNextDeviceTwin(queryTwin);
				list.add(new IoTDevice(deviceTwinDevice.getDeviceId()));
			}
		} catch (Exception e) {
			LOG.error("Error while retrieving devices", e);
		}
		return list;
	}
	
	public void deleteDevice(String deviceId) {
		try {			
			registryManager.removeDevice(deviceId);
		} catch (Exception e) {
			LOG.error("Error while removing device", e);
		}
	}
	
	public Device createDevice(String deviceId) {
		try {
			Device device = Device.createFromId(deviceId, null, null);
			registryManager.addDevice(device);
			setPlatformTag(deviceId);
			return device;
		} catch (Exception e) {
			LOG.error("Error while creating device", e);
		}
		return null;
	}

	private void setPlatformTag(String deviceId) {
		try {
			Set<Pair> tags = new HashSet<>();
			tags.add(new Pair(azureProperties.getTagPlatformKey(), azureProperties.getTagPlatformValue()));
			DeviceTwinDevice deviceTwinDevice = new DeviceTwinDevice(deviceId);
			deviceTwinDevice.setTags(tags);
			deviceTwin.updateTwin(deviceTwinDevice);
		} catch (Exception e) {
			LOG.error("Error while creating device", e);
		}
	}
}