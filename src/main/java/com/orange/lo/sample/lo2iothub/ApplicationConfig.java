/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.RegistryClient;
import com.microsoft.azure.sdk.iot.service.twin.TwinClient;
import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.DeviceClientManager;
import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.exceptions.InitializationException;
import com.orange.lo.sample.lo2iothub.lo.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import com.orange.lo.sdk.LOApiClient;
import com.orange.lo.sdk.LOApiClientParameters;
import com.orange.lo.sdk.rest.model.Device;
import com.orange.lo.sdk.rest.model.Group;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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

import net.jodah.failsafe.RetryPolicy;

@EnableIntegration
@Configuration
public class ApplicationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counters;
    private ApplicationProperties applicationProperties;
    private MessageSender messageSender;
    private ObjectMapper objectMapper;
    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;

    public ApplicationConfig(Counters counterProvider, MessageSender messageSender,
            ApplicationProperties applicationProperties, MappingJackson2HttpMessageConverter springJacksonConverter,
            ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint) {
        this.counters = counterProvider;
        this.messageSender = messageSender;
        this.applicationProperties = applicationProperties;
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
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

        messageSender.setMessageRetryPolicy(messageRetryPolicy());

        applicationProperties.getTenantList().forEach(tenantProperties -> {
            LiveObjectsProperties liveObjectsProperties = tenantProperties.getLiveObjectsProperties();
            List<AzureIotHubProperties> azureIotHubList = tenantProperties.getAzureIotHubList();

            azureIotHubList.forEach(azureIotHubProperties -> {
                try {
                    LOG.debug("Initializing for {} ", azureIotHubProperties.getIotHostName());
                    IoTDeviceProvider ioTDeviceProvider = createIotDeviceProvider(azureIotHubProperties);

                    DeviceClientManager deviceClientManager = new DeviceClientManager(azureIotHubProperties.getIotHostName());
                    
                    IotHubAdapter iotHubAdapter = new IotHubAdapter(
                            ioTDeviceProvider, 
                            messageSender, 
                            deviceClientManager,
                            liveObjectsProperties.isDeviceSynchronization()
                    );

                    LOApiClientParameters loApiClientParameters = loApiClientParameters(liveObjectsProperties,
                            azureIotHubProperties, iotHubAdapter, liveObjectsProperties.isDeviceSynchronization());
                    LOApiClient loApiClient = new LOApiClient(loApiClientParameters);
                    connectorHealthActuatorEndpoint.addLoApiClient(loApiClient);
                    LoAdapter loAdapter = new LoAdapter(loApiClient, liveObjectsProperties.getPageSize(),
                            groupRetryPolicy, deviceRetryPolicy);
                    
                    LoCommandSender loCommandSender = new LoCommandSender(loApiClient, objectMapper, commandRetryPolicy);
                    deviceClientManager.setLoCommandSender(loCommandSender);

                    if (liveObjectsProperties.isDeviceSynchronization()) {
                        DeviceSynchronizationTask deviceSynchronizationTask = new DeviceSynchronizationTask(
                                iotHubAdapter, loAdapter, azureIotHubProperties);
                        int synchronizationDeviceInterval = liveObjectsProperties.getSynchronizationDeviceInterval();
                        Duration period = Duration.ofSeconds(synchronizationDeviceInterval);
                        taskScheduler.scheduleAtFixedRate(deviceSynchronizationTask, period);
                    }

                    loAdapter.startListeningForMessages();
                } catch (IOException | IotHubClientException e) {
                    throw new InitializationException(e);
                }
            });
        });
    }

    private IoTDeviceProvider createIotDeviceProvider(AzureIotHubProperties azureIotHubProperties) throws IOException {
        String iotConnectionString = azureIotHubProperties.getIotConnectionString();
        TwinClient twinClient = new TwinClient(iotConnectionString);
        RegistryClient registryClient = new RegistryClient(iotConnectionString);
        String tagPlatformKey = azureIotHubProperties.getTagPlatformKey();
        String tagPlatformValue = azureIotHubProperties.getTagPlatformValue();
        return new IoTDeviceProvider(twinClient, registryClient, tagPlatformKey, tagPlatformValue);
    }

    private LOApiClientParameters loApiClientParameters(LiveObjectsProperties loProperties,
            AzureIotHubProperties azureIotHubProperties, IotHubAdapter iotHubAdapter, boolean deviceSynchronization) {

        List<String> topics = Lists.newArrayList(azureIotHubProperties.getLoMessagesTopic());
        if (loProperties.isDeviceSynchronization()) {
            topics.add(azureIotHubProperties.getLoDevicesTopic());
        }
        return LOApiClientParameters.builder()
                .hostname(loProperties.getHostname())
                .apiKey(loProperties.getApiKey())
                .automaticReconnect(true)
                .messageQos(loProperties.getQos())
                .keepAliveIntervalSeconds(loProperties.getKeepAliveIntervalSeconds())
                .connectionTimeout(loProperties.getConnectionTimeout())
                .mqttPersistenceDataDir(loProperties.getMqttPersistenceDir())
                .topics(topics)
                .dataManagementMqttCallback(new MessageHandler(iotHubAdapter, counters))
                .connectorType(loProperties.getConnectorType())
                .connectorVersion(getConnectorVersion())
                .build();
    }

    public RetryPolicy<Void> messageRetryPolicy() {
        return new RetryPolicy<Void>().handleIf(e -> e instanceof IllegalStateException)
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .withMaxDuration(Duration.ofHours(1));
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
                .withMaxDuration(Duration.ofHours(1)
        );
    }

    private boolean isTooManyRequestsException(Throwable e) {
        return e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS);
    }

    private String getConnectorVersion() {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            if ((new File("pom.xml")).exists()) {
                model = reader.read(new FileReader("pom.xml"));
            } else {
                model = reader.read(new InputStreamReader(ApplicationConfig.class
                        .getResourceAsStream("/META-INF/maven/com.orange.lo.sample.lo2iot/lo2iot/pom.xml")));
            }
            return model.getVersion().replace(".", "_");
        } catch (Exception e) {
            return "";
        }
    }
}