package com.orange.lo.sample.lo2iothub;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationPropertiesTest {

    @Test
    void shouldSetTenantList() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        List<TenantProperties> tenantList = Collections.singletonList(new TenantProperties());
        applicationProperties.setTenantList(tenantList);

        assertNotNull(applicationProperties.getTenantList());
        assertEquals(tenantList, applicationProperties.getTenantList());
    }
}