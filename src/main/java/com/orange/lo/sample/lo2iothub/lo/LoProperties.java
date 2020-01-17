/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lo")
public class LoProperties {
	
	private String apiKey;	
	private String deviceUrl;	
	private String uri;
	private String username;
	private String topic;
    private String clientId;
    private int recoveryInterval;
    private int completionTimeout;
    private int connectionTimeout;
    private int qos;
    private int keepAliveIntervalSeconds;

    public String getUri() {
    	return uri;
    }

    public void setUri(String uri) {
    	this.uri = uri;
    }

    public String getApiKey() {
		return apiKey;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public int getRecoveryInterval() {
		return recoveryInterval;
	}

	public void setRecoveryInterval(int recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public int getCompletionTimeout() {
		return completionTimeout;
	}

	public void setCompletionTimeout(int completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public int getKeepAliveIntervalSeconds() {
		return keepAliveIntervalSeconds;
	}

	public void setKeepAliveIntervalSeconds(int keepAliveIntervalSeconds) {
		this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getDeviceUrl() {
		return deviceUrl;
	}

	public void setDeviceUrl(String deviceUrl) {
		this.deviceUrl = deviceUrl;
	}
}
