/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import com.orange.lo.sample.lo2iothub.utils.Counters;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.LOApiClientParameters;
import com.orange.lo.sdk.rest.model.Device;
import com.orange.lo.sdk.rest.model.Group;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.HttpClientErrorException;

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
        RetryPolicy<Void> commandRetryPolicy = restCommandRetryPolicy();
        TaskScheduler taskScheduler = taskScheduler();
        RetryPolicy<List<Group>> groupRetryPolicy = restGroupRetryPolicy();
        RetryPolicy<List<Device>> deviceRetryPolicy = restDeviceRetryPolicy();

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

                    LOApiClientParameters loApiClientParameters = loApiClientParameters(liveObjectsProperties,
                            azureIotHubProperties, iotHubAdapter);
                    LOApiClient loApiClient = new LOApiClient(loApiClientParameters);
                    LoAdapter loAdapter = new LoAdapter(loApiClient, liveObjectsProperties.getPageSize(),
                            groupRetryPolicy, deviceRetryPolicy);
                    LoCommandSender loCommandSender = new LoCommandSender(loApiClient, objectMapper, commandRetryPolicy);
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

    public RetryPolicy<Void> restCommandRetryPolicy() {
        return configureRetryPolicy(new RetryPolicy<>());
    }

    public RetryPolicy<List<Group>> restGroupRetryPolicy() {
        return configureRetryPolicy(new RetryPolicy<>());
    }

    public RetryPolicy<List<Device>> restDeviceRetryPolicy() {
        return configureRetryPolicy(new RetryPolicy<>());
    }

    private <T> RetryPolicy<T> configureRetryPolicy(RetryPolicy<T> retryPolicy) {
        return retryPolicy.handleIf(this::isTooManyRequestsException)
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .withMaxDuration(Duration.ofHours(1));
    }

    private boolean isTooManyRequestsException(Throwable e) {
        return e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS);
    }
}