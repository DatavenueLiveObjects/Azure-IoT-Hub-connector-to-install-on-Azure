package com.orange.lo.sample.lo2iothub.azure;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AzureIotHubPropertiesTest {

    private AzureIotHubProperties azureIotHubProperties;

    @BeforeEach
    void setUp() {
        azureIotHubProperties = new AzureIotHubProperties();
    }

    @Test
    void shouldSetIotHostName() {
        String iotHostName = "iot.hostname.com";
        azureIotHubProperties.setIotHostName(iotHostName);

        assertEquals(iotHostName, azureIotHubProperties.getIotHostName());
    }

    @Test
    void shouldSetIotConnectionString() {
        String iotConnectionString = "HostName=iot.hostname.com;SharedAccessKeyName=accesskeyname;SharedAccessKey=aBCD123=";
        azureIotHubProperties.setIotConnectionString(iotConnectionString);

        assertEquals(iotConnectionString, azureIotHubProperties.getIotConnectionString());
    }

    @Test
    void shouldSetSynchronizationThreadPoolSize() {
        int synchronizationThreadPoolSize = 10;
        azureIotHubProperties.setSynchronizationThreadPoolSize(synchronizationThreadPoolSize);

        assertEquals(synchronizationThreadPoolSize, azureIotHubProperties.getSynchronizationThreadPoolSize());
    }

    @Test
    void shouldSetMessagingThreadPoolSize() {
        int messagingThreadPoolSize = 20;
        azureIotHubProperties.setMessagingThreadPoolSize(messagingThreadPoolSize);

        assertEquals(messagingThreadPoolSize, azureIotHubProperties.getMessagingThreadPoolSize());
    }

    @Test
    void shouldSetDeviceClientConnectionTimeout() {
        int deviceClientConnectionTimeout = 3000;
        azureIotHubProperties.setDeviceClientConnectionTimeout(deviceClientConnectionTimeout);

        assertEquals(deviceClientConnectionTimeout, azureIotHubProperties.getDeviceClientConnectionTimeout());
    }

    @Test
    void shouldSetTagPlatformKey() {
        String platform = "platform";
        azureIotHubProperties.setTagPlatformKey(platform);

        assertEquals(platform, azureIotHubProperties.getTagPlatformKey());
    }

    @Test
    void shouldSetTagPlatformValue() {
        String platformValue = "platformValue";
        azureIotHubProperties.setTagPlatformValue(platformValue);

        assertEquals(platformValue, azureIotHubProperties.getTagPlatformValue());
    }

    @Test
    void shouldSetLoMessagesTopic() {
        String loMessagesTopic = "message-topic-lo";
        azureIotHubProperties.setLoMessagesTopic(loMessagesTopic);

        assertEquals(loMessagesTopic, azureIotHubProperties.getLoMessagesTopic());
    }

    @Test
    void shouldSetLoDevicesTopic() {
        String loDevicesTopic = "device-topic-lo";
        azureIotHubProperties.setLoDevicesTopic(loDevicesTopic);

        assertEquals(loDevicesTopic, azureIotHubProperties.getLoDevicesTopic());
    }

    @Test
    void shouldSetLoDevicesGroup() {
        String loDevicesGroup = "device-group-lo";
        azureIotHubProperties.setLoDevicesGroup(loDevicesGroup);

        assertEquals(loDevicesGroup, azureIotHubProperties.getLoDevicesGroup());
    }
}