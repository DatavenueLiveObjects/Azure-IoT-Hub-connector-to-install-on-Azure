package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.scheduling.TaskScheduler;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private Counters counterProvider;
    @Mock
    private IntegrationFlowContext integrationflowContext;
    @Mock
    private MessageSender messageSender;
    @Mock
    private ApplicationProperties applicationProperties;

    private ApplicationConfig applicationConfig;

    @BeforeEach
    void setUp() {
//        AzureIotHubProperties azureIotHubProperties = new AzureIotHubProperties();
//        List<AzureIotHubProperties> azureIotHubList = Collections.singletonList(azureIotHubProperties);
//        LiveObjectsProperties liveObjectsProperties = new LiveObjectsProperties();
//        TenantProperties tenantProperties = new TenantProperties();
//        tenantProperties.setAzureIotHubList(azureIotHubList);
//        tenantProperties.setLiveObjectsProperties(liveObjectsProperties);
//        ApplicationProperties applicationProperties = new ApplicationProperties();
//        applicationProperties.setTenantList(Collections.singletonList(tenantProperties));
        applicationConfig = new ApplicationConfig(counterProvider, integrationflowContext, messageSender, applicationProperties);
    }

    @Test
    void shouldCreateTaskScheduler() {
        TaskScheduler taskScheduler = applicationConfig.taskScheduler();
        assertNotNull(taskScheduler);
    }

//    @Test
//    void init() {
//        applicationConfig.init();
//
//    }
}