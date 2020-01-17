/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.microsoft.azure.sdk.iot.device.DeviceClient;

@Component
public class IotClientCache {

	private Map<String, DeviceClient> map = new ConcurrentHashMap<>();
	
	public DeviceClient add(String deviceId, DeviceClient client) {
		return map.put(deviceId, client);
	}
	
	public DeviceClient get(String deviceId) {
		return map.get(deviceId);
	}
	
	public boolean remove(String deviceId) {
		try {
			DeviceClient client = map.remove(deviceId);
			if (client != null) {
				client.registerConnectionStatusChangeCallback(null, null);
				client.closeNow();
				return true;
			}
		} catch (IOException e) {
			// doesn't matter
		}
		return false;
	}
	
	public Set<String> getDeviceIds() {
		return map.keySet();
	}
}
