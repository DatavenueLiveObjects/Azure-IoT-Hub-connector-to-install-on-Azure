package com.orange.lo.sample.lo2iothub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveObjectsPropertiesTest {

    private LiveObjectsProperties liveObjectsProperties;

    @BeforeEach
    void setUp() {
        liveObjectsProperties = new LiveObjectsProperties();
    }

    @Test
    void shouldSetUri() {
        String uri = "ssl://liveobjects.orange-business.com:8883";
        liveObjectsProperties.setUri(uri);

        assertEquals(uri, liveObjectsProperties.getUri());
    }

    @Test
    void shouldSetUsername() {
        String username = "application";
        liveObjectsProperties.setUsername(username);

        assertEquals(username, liveObjectsProperties.getUsername());
    }

    @Test
    void shouldSetClientId() {
        String clientId = "mqtt2iot";
        liveObjectsProperties.setClientId(clientId);

        assertEquals(clientId, liveObjectsProperties.getClientId());
    }

    @Test
    void shouldSetRecoveryInterval() {
        int recoveryInterval = 1000;
        liveObjectsProperties.setRecoveryInterval(recoveryInterval);

        assertEquals(recoveryInterval, liveObjectsProperties.getRecoveryInterval());
    }

    @Test
    void shouldSetCompletionTimeout() {
        int completionTimeout = 2000;
        liveObjectsProperties.setCompletionTimeout(completionTimeout);

        assertEquals(completionTimeout, liveObjectsProperties.getCompletionTimeout());
    }

    @Test
    void shouldSetConnectionTimeout() {
        int connectionTimeout = 3000;
        liveObjectsProperties.setConnectionTimeout(connectionTimeout);

        assertEquals(connectionTimeout, liveObjectsProperties.getConnectionTimeout());
    }

    @Test
    void shouldSetQos() {
        int qos = 0;
        liveObjectsProperties.setQos(qos);

        assertEquals(qos, liveObjectsProperties.getQos());
    }

    @Test
    void shouldSetKeepAliveIntervalSeconds() {
        int keepAliveIntervalSeconds = 4000;
        liveObjectsProperties.setKeepAliveIntervalSeconds(keepAliveIntervalSeconds);

        assertEquals(keepAliveIntervalSeconds, liveObjectsProperties.getKeepAliveIntervalSeconds());
    }

    @Test
    void shouldSetApiKey() {
        String apiKey = "aBcDEF132";
        liveObjectsProperties.setApiKey(apiKey);

        assertEquals(apiKey, liveObjectsProperties.getApiKey());
    }

    @Test
    void shouldSetPageSize() {
        int pageSize = 20;
        liveObjectsProperties.setPageSize(pageSize);

        assertEquals(pageSize, liveObjectsProperties.getPageSize());
    }

    @Test
    void shouldSetApiUrl() {
        String apiUrl = "https://liveobjects.orange-business.com/api";
        liveObjectsProperties.setApiUrl(apiUrl);

        assertEquals(apiUrl, liveObjectsProperties.getApiUrl());
    }

    @Test
    void shouldSetSynchronizationDeviceInterval() {
        int synchronizationDeviceInterval = 2500;
        liveObjectsProperties.setSynchronizationDeviceInterval(synchronizationDeviceInterval);

        assertEquals(synchronizationDeviceInterval, liveObjectsProperties.getSynchronizationDeviceInterval());
    }
}