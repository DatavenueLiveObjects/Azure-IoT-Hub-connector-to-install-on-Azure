/**
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;
import com.orange.lo.sample.lo2iothub.azure.IotClientCache;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.exceptions.InitializationException;
import com.orange.lo.sample.lo2iothub.lo.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.orange.lo.sample.lo2iothub.utils.Counters;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.LOApiClientParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@EnableIntegration
@Configuration
public class ApplicationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;
    private ApplicationProperties applicationProperties;
    private MessageSender messageSender;
    private ObjectMapper objectMapper;

    public ApplicationConfig(Counters counterProvider, MessageSender messageSender,
                             ApplicationProperties applicationProperties,
                             MappingJackson2HttpMessageConverter springJacksonConverter) {
        this.counterProvider = counterProvider;
        this.messageSender = messageSender;
        this.applicationProperties = applicationProperties;
        this.objectMapper = springJacksonConverter.getObjectMapper();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public void init() {
        TaskScheduler taskScheduler = taskScheduler();

        applicationProperties.getTenantList().forEach(tenantProperties -> {
            LiveObjectsProperties liveObjectsProperties = tenantProperties.getLiveObjectsProperties();
            List<AzureIotHubProperties> azureIotHubList = tenantProperties.getAzureIotHubList();

            azureIotHubList.forEach(azureIotHubProperties -> {
                try {
                    LOG.debug("Initializing for {} ", azureIotHubProperties.getIotHostName());
                    IoTDeviceProvider ioTDeviceProvider = createIotDeviceProvider(azureIotHubProperties);
                    IotClientCache iotClientCache = new IotClientCache();

                    IotHubAdapter iotHubAdapter = new IotHubAdapter(
                            ioTDeviceProvider,
                            messageSender,
                            iotClientCache,
                            azureIotHubProperties
                    );

                    LOApiClientParameters loApiClientParameters = loApiClientParameters(liveObjectsProperties, azureIotHubProperties, iotHubAdapter);
                    LOApiClient loApiClient = new LOApiClient(loApiClientParameters);
                    LoAdapter loAdapter = new LoAdapter(loApiClient, liveObjectsProperties.getPageSize());
                    LoCommandSender loCommandSender = new LoCommandSender(loApiClient, objectMapper);
                    iotHubAdapter.setLoCommandSender(loCommandSender);

                    DeviceSynchronizationTask deviceSynchronizationTask = new DeviceSynchronizationTask(iotHubAdapter,
                            loAdapter, azureIotHubProperties);
                    int synchronizationDeviceInterval = liveObjectsProperties.getSynchronizationDeviceInterval();
                    Duration period = Duration.ofSeconds(synchronizationDeviceInterval);
                    taskScheduler.scheduleAtFixedRate(deviceSynchronizationTask, period);

                } catch (IOException e) {
                    throw new InitializationException(e);
                }
            });

        });
    }

    private IoTDeviceProvider createIotDeviceProvider(AzureIotHubProperties azureIotHubProperties) throws IOException {
        String iotConnectionString = azureIotHubProperties.getIotConnectionString();
        DeviceTwin deviceTwin = DeviceTwin.createFromConnectionString(iotConnectionString);
        RegistryManager registryManager = RegistryManager.createFromConnectionString(iotConnectionString);
        String tagPlatformKey = azureIotHubProperties.getTagPlatformKey();
        String tagPlatformValue = azureIotHubProperties.getTagPlatformValue();
        return new IoTDeviceProvider(deviceTwin, registryManager, tagPlatformKey, tagPlatformValue);
    }

    private LOApiClientParameters loApiClientParameters(LiveObjectsProperties loProperties,
                                                        AzureIotHubProperties azureIotHubProperties,
                                                        IotHubAdapter iotHubAdapter) {
        String loDevicesTopic = azureIotHubProperties.getLoDevicesTopic();
        String loMessagesTopic = azureIotHubProperties.getLoMessagesTopic();

        return LOApiClientParameters.builder()
                .hostname(loProperties.getHostname())
                .apiKey(loProperties.getApiKey())
                .automaticReconnect(true)
                .messageQos(loProperties.getQos())
                .keepAliveIntervalSeconds(loProperties.getKeepAliveIntervalSeconds())
                .connectionTimeout(loProperties.getConnectionTimeout())
                .mqttPersistenceDataDir(loProperties.getMqttPersistenceDir())
                .topics(Arrays.asList(loDevicesTopic, loMessagesTopic))
                .dataManagementMqttCallback(new MessageHandler(iotHubAdapter, counterProvider))
                .build();
    }
}