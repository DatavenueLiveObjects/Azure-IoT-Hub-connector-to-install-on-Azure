package com.orange.lo.sample.lo2iothub.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "azure")
public class AzureProperties {
	
	private String iotConnectionString;
	private String iotHostName;	
	private int synchronizationThreadPoolSize;
	private int messagingThreadPoolSize;
	private int deviceClientconnectionTimeout;
	private String tagPlatformKey; 
	private String tagPlatformValue;

	public String getIotHostName() {
		return iotHostName;
	}

	public void setIotHostName(String iotHostName) {
		this.iotHostName = iotHostName;
	}

	public String getIotConnectionString() {
		return iotConnectionString;
	}

	public void setIotConnectionString(String iotConnectionString) {
		this.iotConnectionString = iotConnectionString;
	}

	public int getSynchronizationThreadPoolSize() {
		return synchronizationThreadPoolSize;
	}

	public void setSynchronizationThreadPoolSize(int synchronizationThreadPoolSize) {
		this.synchronizationThreadPoolSize = synchronizationThreadPoolSize;
	}
	
	public int getMessagingThreadPoolSize() {
		return messagingThreadPoolSize;
	}

	public void setMessagingThreadPoolSize(int messagingThreadPoolSize) {
		this.messagingThreadPoolSize = messagingThreadPoolSize;
	}

	public int getDeviceClientconnectionTimeout() {
		return deviceClientconnectionTimeout;
	}

	public void setDeviceClientconnectionTimeout(int deviceClientconnectionTimeout) {
		this.deviceClientconnectionTimeout = deviceClientconnectionTimeout;
	}

	public String getTagPlatformKey() {
		return tagPlatformKey;
	}

	public void setTagPlatformKey(String tagPlatformKey) {
		this.tagPlatformKey = tagPlatformKey;
	}

	public String getTagPlatformValue() {
		return tagPlatformValue;
	}

	public void setTagPlatformValue(String tagPlatformValue) {
		this.tagPlatformValue = tagPlatformValue;
	}
}