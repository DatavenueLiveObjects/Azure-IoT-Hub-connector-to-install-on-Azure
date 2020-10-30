package com.orange.lo.sample.lo2iothub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantPropertiesTest {

    private TenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
    }

    @Test
    void shouldSetAzureIotHubList() {
        List<AzureIotHubProperties> azureIotHubProperties = Collections.singletonList(new AzureIotHubProperties());
        tenantProperties.setAzureIotHubList(azureIotHubProperties);

        assertEquals(azureIotHubProperties.size(), tenantProperties.getAzureIotHubList().size());
        assertEquals(azureIotHubProperties, tenantProperties.getAzureIotHubList());
    }

    @Test
    void shouldSetLiveObjectsProperties() {
        LiveObjectsProperties liveObjectsProperties = new LiveObjectsProperties();
        tenantProperties.setLiveObjectsProperties(liveObjectsProperties);

        assertEquals(liveObjectsProperties, tenantProperties.getLiveObjectsProperties());
    }
}