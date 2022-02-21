/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.lo.LiveObjectsProperties;
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
        LiveObjectsProperties LOProperties = new LiveObjectsProperties();
        tenantProperties.setLiveObjectsProperties(LOProperties);

        assertEquals(LOProperties, tenantProperties.getLiveObjectsProperties());
    }
}