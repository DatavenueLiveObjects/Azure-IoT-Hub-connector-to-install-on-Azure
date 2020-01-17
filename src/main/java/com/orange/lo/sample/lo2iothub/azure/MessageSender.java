/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.orange.lo.sample.lo2iothub.Counters;
import com.orange.lo.sample.lo2iothub.IotDeviceManagement;

@Component
public class MessageSender {

	private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private IotClientCache iotClientCache;
	private IotDeviceManagement iotDeviceManagement;
    private ThreadPoolExecutor sendingExecutor;
    private Counters counterProvider;
    
	@Autowired
	public MessageSender(IotClientCache iotClientCache, AzureProperties azureProperties, IotDeviceManagement iotDeviceManagement, Counters counterProvider) {
		this.iotClientCache = iotClientCache;
		this.iotDeviceManagement = iotDeviceManagement;
		this.counterProvider = counterProvider;
		BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
		sendingExecutor = new ThreadPoolExecutor(azureProperties.getMessagingThreadPoolSize(), azureProperties.getMessagingThreadPoolSize(), 10, TimeUnit.SECONDS, tasks);
	}
	
	public void send(Message<String> msg) {
	    try {
			String loClientId = getSourceDeviceId(msg.getPayload());
			DeviceClient deviceClient = iotClientCache.get(loClientId);
			if (deviceClient != null)
				sendMessage(msg, deviceClient);
            else {
                sendingExecutor.submit(() -> {
                	DeviceClient createdDeviceClient = iotDeviceManagement.createDeviceClient(loClientId);
					sendMessage(msg, createdDeviceClient);
                });
            }
	    }
		catch (IllegalArgumentException e) {
			LOG.error("Error while sending message", e);
		} catch (JSONException e) {
			LOG.error("Cannot retrieve device id from message, message not sent {}", msg.getPayload());
		}
	}

	private void sendMessage(Message<String> msg, DeviceClient deviceClient) {
		counterProvider.evtAttempt().increment();		
		com.microsoft.azure.sdk.iot.device.Message message = new com.microsoft.azure.sdk.iot.device.Message(msg.getPayload());
		deviceClient.sendEventAsync(message, new EventCallback(), message);
	}

	private String getSourceDeviceId(String msg) throws JSONException {
		return new JSONObject(msg).getJSONObject("metadata").getString("source");
	}

	protected class EventCallback implements IotHubEventCallback {
		public void execute(IotHubStatusCode status, Object context){
			
			switch (status) {
				case OK: 
				case OK_EMPTY:
					counterProvider.evtSent().increment();
					break;
				default:
					counterProvider.evtFailed().increment();
					break;
			}
			
			if (LOG.isDebugEnabled()) {
				com.microsoft.azure.sdk.iot.device.Message msg = (com.microsoft.azure.sdk.iot.device.Message) context;
				LOG.debug("IoT Hub responded to message " + msg.getMessageId()  + " with status " + status.name());				
			}
		}
	}
}