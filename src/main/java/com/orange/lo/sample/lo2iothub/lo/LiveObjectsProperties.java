/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo;

public class LiveObjectsProperties {

    private static final String CONNECTOR_TYPE = "LO_AZURE_IOTHUB_ADAPTER";

    private String hostname;
    private String username;
    private String apiKey;
    private String mqttPersistenceDir;
    private int connectionTimeout;
    private int qos;
    private int keepAliveIntervalSeconds;
    private int pageSize;
    private boolean deviceSynchronization;
    private int deviceSynchronizationInterval;

    public String getConnectorType() {
        return CONNECTOR_TYPE;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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

    public String getMqttPersistenceDir() {
        return mqttPersistenceDir;
    }

    public void setMqttPersistenceDir(String mqttPersistenceDir) {
        this.mqttPersistenceDir = mqttPersistenceDir;
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

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getDeviceSynchronizationInterval() {
        return deviceSynchronizationInterval;
    }

    public void setDeviceSynchronizationInterval(int deviceSynchronizationInterval) {
        this.deviceSynchronizationInterval = deviceSynchronizationInterval;
    }

    public boolean isDeviceSynchronization() {
        return deviceSynchronization;
    }

    public void setDeviceSynchronization(boolean deviceSynchronization) {
        this.deviceSynchronization = deviceSynchronization;
    }
}
