/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

import com.microsoft.applicationinsights.autoconfigure.ApplicationInsightsProperties;
import com.orange.lo.sample.lo2iothub.ApplicationProperties;
import com.orange.lo.sample.lo2iothub.ConnectorApplication;
import com.orange.lo.sample.lo2iothub.TenantProperties;
import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.lo.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyAzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyConfigurationProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyLiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyTenantProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ModifyConfigurationServiceTest {

    private static final String INSTRUMENTATION_KEY = "key";
    private static final String INSTRUMENTATION_KEY_UPDATED = "key_updated";

    private static final String LO_API_KEY = "abcd";
    private static final String LO_API_KEY_UPDATED = "dcba";

    private static final String LO_MESSAGE_TOPIC = "topic";
    private static final String LO_MESSAGE_TOPIC_UPDATED = "topic_updated";

    @TempDir
    File tempDir;

    private File configurationFile;

    private ModifyConfigurationService modifyConfigurationService;

    @BeforeEach
    void setUp() throws IOException {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        TenantProperties tp = new TenantProperties();
        LiveObjectsProperties liveObjectsProperties = new LiveObjectsProperties();
        liveObjectsProperties.setApiKey(LO_API_KEY);
        tp.setLiveObjectsProperties(liveObjectsProperties);

        AzureIotHubProperties azureIotHubProperties = new AzureIotHubProperties();
        azureIotHubProperties.setLoMessagesTopic(LO_MESSAGE_TOPIC);
        tp.setAzureIotHubList(Arrays.asList(azureIotHubProperties));

        applicationProperties.setTenantList(Arrays.asList(tp));

        ApplicationInsightsProperties applicationInsightsProperties = new ApplicationInsightsProperties();
        applicationInsightsProperties.setInstrumentationKey(INSTRUMENTATION_KEY);

        configurationFile = new File(tempDir, "application.yml");
        FileUtils.fileWrite(configurationFile, "tenant-list:\n  - \n    live-objects-properties:\n      api-key: " + LO_API_KEY + "\n    azure-iot-hub-list:\n      -\n        lo-messages-topic: " + LO_MESSAGE_TOPIC + "\nazure:\n  application-insights:\n    instrumentation-key: " + INSTRUMENTATION_KEY);

        modifyConfigurationService = new ModifyConfigurationService(applicationProperties, applicationInsightsProperties, configurationFile);
    }

    @Test
    public void shouldReadParameters() {
        // given
        // when(profileCredentialsProvider.getCredentials()).thenReturn(getAWSCredentials());

        // when
        ModifyConfigurationProperties properties = modifyConfigurationService.getProperties();

        // then
        Assertions.assertEquals(INSTRUMENTATION_KEY, properties.getApplicationInsights());
        Assertions.assertEquals(LO_API_KEY, properties.getTenantList().get(0).getLiveObjectsProperties().getLoApiKey());
        Assertions.assertEquals(LO_MESSAGE_TOPIC, properties.getTenantList().get(0).getAzureIotHubList().get(0).getLoMessagesTopic());
    }

    @Test
    public void shouldUpdateParameters() throws IOException {
        // given
        MockedStatic<ConnectorApplication> connectorApplication = Mockito.mockStatic(ConnectorApplication.class);

        ModifyConfigurationProperties modifyConfigurationProperties = new ModifyConfigurationProperties();
        modifyConfigurationProperties.setApplicationInsights(INSTRUMENTATION_KEY_UPDATED);

        ModifyTenantProperties modifyTenantProperties = new ModifyTenantProperties();
        ModifyLiveObjectsProperties modifyLiveObjectsProperties = new ModifyLiveObjectsProperties();
        modifyLiveObjectsProperties.setLoApiKey(LO_API_KEY_UPDATED);
        modifyTenantProperties.setLiveObjectsProperties(modifyLiveObjectsProperties);

        ModifyAzureIotHubProperties modifyAzureIotHubProperties = new ModifyAzureIotHubProperties();
        modifyAzureIotHubProperties.setLoMessagesTopic(LO_MESSAGE_TOPIC_UPDATED);
        modifyTenantProperties.setAzureIotHubList(Arrays.asList(modifyAzureIotHubProperties));
        modifyConfigurationProperties.setTenantList(Arrays.asList(modifyTenantProperties));

        // when
        modifyConfigurationService.modify(modifyConfigurationProperties);

        // then
        String configuratioFileContent = FileUtils.fileRead(configurationFile);

        connectorApplication.verify(ConnectorApplication::restart);
        Assertions.assertTrue(configuratioFileContent.contains(LO_API_KEY_UPDATED));
        Assertions.assertTrue(configuratioFileContent.contains(LO_MESSAGE_TOPIC_UPDATED));
        Assertions.assertTrue(configuratioFileContent.contains(INSTRUMENTATION_KEY_UPDATED));
    }
}