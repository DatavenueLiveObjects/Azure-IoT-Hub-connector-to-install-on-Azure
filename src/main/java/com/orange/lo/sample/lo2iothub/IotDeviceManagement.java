package com.orange.lo.sample.lo2iothub;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.RetryDecision;
import com.microsoft.azure.sdk.iot.service.Device;
import com.orange.lo.sample.lo2iothub.azure.AzureProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;
import com.orange.lo.sample.lo2iothub.azure.IotClientCache;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.lo.LoDeviceProvider;

@EnableScheduling
@Component
public class IotDeviceManagement {

	private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";
	
	private IoTDeviceProvider ioTDeviceProvider;
	private LoDeviceProvider loDeviceProvider;
	private MessageProducerSupport mqttInbound;
	private LoCommandSender loCommandSender;
	private AzureProperties azureProperties;
	private IotClientCache iotClientCache;

	public IotDeviceManagement(IoTDeviceProvider ioTDeviceProvider, LoDeviceProvider loDeviceProvider, AzureProperties azureProperties, LoCommandSender loCommandSender, IotClientCache iotClientCache, MessageProducerSupport mqttInbound) {
		this.ioTDeviceProvider = ioTDeviceProvider;
		this.loDeviceProvider = loDeviceProvider;
		this.azureProperties = azureProperties;
		this.loCommandSender = loCommandSender;
		this.iotClientCache = iotClientCache;
		this.mqttInbound = mqttInbound;
	}

	@Scheduled(fixedRateString = "${azure.synchronization-device-interval}")
	public void synchronizeDevices() throws InterruptedException {
        LOG.debug("Synchronizing devices ... ");

        Set<String> loIds = loDeviceProvider.getDevices().stream().map(d -> d.getId()).collect(Collectors.toSet());
        
        ThreadPoolExecutor synchronizingExecutor = new ThreadPoolExecutor(azureProperties.getSynchronizationThreadPoolSize(), 
        																  azureProperties.getSynchronizationThreadPoolSize(), 
        																  10, 
        																  TimeUnit.SECONDS, 
        																  new ArrayBlockingQueue<Runnable>(loIds.size()));
        
        for (String deviceId : loIds) {
			synchronizingExecutor.execute(() -> {
				createDeviceClient(deviceId);
			});
		}
        
        int synchronizationTimeout = calculateSynchronizationTimeout(loIds.size(), azureProperties.getSynchronizationThreadPoolSize());
        synchronizingExecutor.shutdown();
        if (synchronizingExecutor.awaitTermination(synchronizationTimeout, TimeUnit.SECONDS)) {
        	Set<String> iotIds = ioTDeviceProvider.getDevices().stream().map(d -> d.getId()).collect(Collectors.toSet());
        	iotIds.removeAll(loIds);
        	iotIds.forEach(id -> {
        		LOG.debug("remove from cache and iot device " + id);
        		iotClientCache.remove(id);
        		ioTDeviceProvider.deleteDevice(id);
        	});
        }
        mqttInbound.start();
	}
	
	public DeviceClient createDeviceClient(String deviceId) {
		synchronized (deviceId.intern()) {
			Device device = ioTDeviceProvider.getDevice(deviceId);
			// no device in iot hub
			if (device == null) {
				device = ioTDeviceProvider.createDevice(deviceId);
				DeviceClient deviceClient = iotClientCache.get(deviceId);
				// make sure that if device client exists we have to close it  
				if (deviceClient != null) {
					iotClientCache.remove(deviceId);
				}
				return createDeviceClient(device);
			// device exists but device client doesn't
			} else if (iotClientCache.get(deviceId) == null) {
				return createDeviceClient(device);
			} else {
				return iotClientCache.get(deviceId);
			}
		}
	}

	private DeviceClient createDeviceClient(Device device) {
		if (iotClientCache.get(device.getDeviceId()) == null ) {
			String connString = String.format(CONNECTION_STRING_PATTERN, azureProperties.getIotHostName(), device.getDeviceId(), device.getSymmetricKey().getPrimaryKey());
			try {
				DeviceClient deviceClient = new DeviceClient(connString, IotHubClientProtocol.MQTT);
				deviceClient.setMessageCallback(new MessageCallbackMqtt(), device.getDeviceId());
				deviceClient.setOperationTimeout(azureProperties.getDeviceClientconnectionTimeout());
				deviceClient.setRetryPolicy((currentRetryCount,lastException) -> new RetryDecision(false, 0));
				deviceClient.registerConnectionStatusChangeCallback(new ConnectionStatusChangeCallback(), device.getDeviceId());
				
				deviceClient.open();
				iotClientCache.add(device.getDeviceId(), deviceClient);
				LOG.info("Device client created for {}", device.getDeviceId());
				return deviceClient;
			} catch (URISyntaxException | IOException e) {
				LOG.error("Error while creating device client", e);
				return null;
			}
		}
		return iotClientCache.get(device.getDeviceId());
	}
	
	private int calculateSynchronizationTimeout(int devices, int threadPoolSize) {
		return (devices / threadPoolSize + 1) * 5; // max 5 seconds for 1 operation
	}
	
	protected class ConnectionStatusChangeCallback implements IotHubConnectionStatusChangeCallback {

		@Override
		public void execute(IotHubConnectionStatus status, IotHubConnectionStatusChangeReason statusChangeReason,
				Throwable throwable, Object callbackContext) {
			
			if (IotHubConnectionStatus.DISCONNECTED == status) {
				String deviceId = callbackContext.toString();				
				LOG.debug("Device client disconnected for {}, trying to recreate ...", deviceId);
				iotClientCache.remove(deviceId);
				createDeviceClient(deviceId);
			}
		}
	}
	
	protected class MessageCallbackMqtt implements MessageCallback {
        public IotHubMessageResult execute(Message msg, Object context) {
            String deviceId = context.toString();
            
            if (LOG.isDebugEnabled()) {
            	LOG.debug("Received message for device: {} with content {}", deviceId, new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
            	for (MessageProperty messageProperty : msg.getProperties()){
            		LOG.debug(messageProperty.getName() + " : " + messageProperty.getValue());
            	}            	
            }
            
            loCommandSender.send(deviceId, new String(msg.getBytes()));
            return IotHubMessageResult.COMPLETE;
        }
    }
}