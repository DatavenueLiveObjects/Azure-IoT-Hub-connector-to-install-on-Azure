package com.orange.lo.sample.lo2iothub.modify;

import com.orange.lo.sample.lo2iothub.modify.model.ModifyAzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyConfigurationProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyLiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyTenantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

@Component
public class ModifyConfigurationService {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ModifyConfigurationService() {
    }

    public ModifyConfigurationProperties getProperties() {

        return new ModifyConfigurationProperties();
    }

    public void modify(ModifyConfigurationProperties modifyConfigurationProperties) {
        LOG.info(modifyConfigurationProperties.toString());
    }
}