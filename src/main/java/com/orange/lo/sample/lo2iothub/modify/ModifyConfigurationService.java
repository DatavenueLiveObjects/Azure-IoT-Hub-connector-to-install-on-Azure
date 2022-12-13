/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.microsoft.applicationinsights.autoconfigure.ApplicationInsightsProperties;
import com.orange.lo.sample.lo2iothub.ApplicationProperties;
import com.orange.lo.sample.lo2iothub.ConnectorApplication;
import com.orange.lo.sample.lo2iothub.lo.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyAzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyConfigurationProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyLiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyTenantProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ModifyConfigurationService {

    private ApplicationProperties applicationProperties;
    private File configurationFile;
    private ApplicationInsightsProperties applicationInsightsProperties;

    public ModifyConfigurationService(ApplicationProperties applicationProperties, ApplicationInsightsProperties applicationInsightsProperties, File configurationFile) {
        this.applicationProperties = applicationProperties;
        this.applicationInsightsProperties = applicationInsightsProperties;
        this.configurationFile = configurationFile;
    }

    public ModifyConfigurationProperties getProperties() {
        ModifyConfigurationProperties modifyConfigurationProperties = new ModifyConfigurationProperties();

        List<ModifyTenantProperties> modifyTenantPropertiesList = applicationProperties.getTenantList().stream().map(tp -> {

            LiveObjectsProperties liveObjectsProperties = tp.getLiveObjectsProperties();
            ModifyLiveObjectsProperties modifyLiveObjectsProperties = new ModifyLiveObjectsProperties();
            modifyLiveObjectsProperties.setLoApiKey(liveObjectsProperties.getApiKey());

            List<ModifyAzureIotHubProperties> modifyAzureIotHubList = tp.getAzureIotHubList().stream().map(aihp -> {
                ModifyAzureIotHubProperties modifyAzureIotHubProperties = new ModifyAzureIotHubProperties();
                modifyAzureIotHubProperties.setIotConnectionString(aihp.getIotConnectionString());
                modifyAzureIotHubProperties.setIotHostName(aihp.getIotHostName());
                modifyAzureIotHubProperties.setLoDevicesGroup(aihp.getLoDevicesGroup());
                modifyAzureIotHubProperties.setLoDevicesTopic(aihp.getLoDevicesTopic());
                modifyAzureIotHubProperties.setLoMessagesTopic(aihp.getLoMessagesTopic());
                return modifyAzureIotHubProperties;
            }).collect(Collectors.toList());

            ModifyTenantProperties modifyTenantProperties = new ModifyTenantProperties();
            modifyTenantProperties.setLiveObjectsProperties(modifyLiveObjectsProperties);
            modifyTenantProperties.setAzureIotHubList(modifyAzureIotHubList);

            return modifyTenantProperties;
        }).collect(Collectors.toList());

        modifyConfigurationProperties.setTenantList(modifyTenantPropertiesList);
        modifyConfigurationProperties.setApplicationInsights(applicationInsightsProperties.getInstrumentationKey());

        return modifyConfigurationProperties;
    }

    public void modify(ModifyConfigurationProperties modifyConfigurationProperties) {

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ObjectNode configurationFileContent = (ObjectNode) mapper.readTree(configurationFile);

            ObjectNode loNode = (ObjectNode) configurationFileContent.get("tenant-list").get(0).get("live-objects-properties");
            ObjectNode iotNode = (ObjectNode) configurationFileContent.get("tenant-list").get(0).get("azure-iot-hub-list").get(0);
            ObjectNode applicationInsightsNode = (ObjectNode) configurationFileContent.get("azure").get("application-insights");

            setProperty(loNode, "api-key", () -> modifyConfigurationProperties.getTenantList().get(0).getLiveObjectsProperties().getLoApiKey());

            setProperty(iotNode, "iot-connection-string", () -> modifyConfigurationProperties.getTenantList().get(0).getAzureIotHubList().get(0).getIotConnectionString());
            setProperty(iotNode, "iot-host-name", () -> modifyConfigurationProperties.getTenantList().get(0).getAzureIotHubList().get(0).getIotHostName());
            setProperty(iotNode, "lo-devices-group", () -> modifyConfigurationProperties.getTenantList().get(0).getAzureIotHubList().get(0).getLoDevicesGroup());
            setProperty(iotNode, "lo-devices-topic", () -> modifyConfigurationProperties.getTenantList().get(0).getAzureIotHubList().get(0).getLoDevicesTopic());
            setProperty(iotNode, "lo-messages-topic", () -> modifyConfigurationProperties.getTenantList().get(0).getAzureIotHubList().get(0).getLoMessagesTopic());

            setProperty(applicationInsightsNode, "instrumentation-key", () -> modifyConfigurationProperties.getApplicationInsights());

            mapper.writer().writeValue(configurationFile, configurationFileContent);
            ConnectorApplication.restart();

        } catch (Exception e) {
            throw new ModifyException("Error while modifying configuration", e);
        }
    }

    private void setProperty(ObjectNode node, String parameterName, Supplier<Object> parameterSupplier) {

        try {
            Object parameter = parameterSupplier.get();
            if (Objects.isNull(parameter)) {
                return;
            }

            if (parameter instanceof Integer) {
                node.put(parameterName, (Integer) parameter);
            } else if (parameter instanceof Long) {
                node.put(parameterName, (Long) parameter);
            } else if (parameter instanceof Boolean) {
                node.put(parameterName, (Boolean) parameter);
            } else {
                node.put(parameterName, String.valueOf(parameter));
            }
        } catch (Exception e) {
            // doesn't matter
        }
    }
}