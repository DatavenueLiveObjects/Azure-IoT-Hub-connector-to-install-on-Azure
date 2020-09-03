/**
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;
import com.orange.lo.sample.lo2iothub.azure.IotClientCache;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.exceptions.InitializationException;
import com.orange.lo.sample.lo2iothub.lo.LoApiClient;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orange.lo.sample.lo2iothub.utils.Counters;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

@EnableIntegration
@Configuration
public class ApplicationConfig {

    private static final String DEVICE_ID_FIELD = "deviceId";
    private static final String TYPE_FIELD = "type";
    private static final String UNKNOW_MESSAGE_TYPE = "unknow";
    private static final String DEVICE_DELETED_MESSAGE_TYPE = "deviceDeleted";
    private static final String DEVICE_CREATED_MESSAGE_TYPE = "deviceCreated";
    private static final String DATA_MESSAGE_TYPE = "dataMessage";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;
    private IntegrationFlowContext integrationFlowContext;
    private ApplicationProperties applicationProperties;
    private MessageSender messageSender;

    @Autowired
    public ApplicationConfig(Counters counterProvider, IntegrationFlowContext integrationflowContext, MessageSender messageSender, ApplicationProperties applicationProperties) {
        this.counterProvider = counterProvider;
        this.integrationFlowContext = integrationflowContext;
        this.messageSender = messageSender;
        this.applicationProperties = applicationProperties;

    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public void test() {
        TaskScheduler taskScheduler = taskScheduler();

        applicationProperties.getTenantList().forEach(tenantProperties -> {
            LiveObjectsProperties liveObjectsProperties = tenantProperties.getLiveObjectsProperties();
            List<AzureIotHubProperties> azureIotHubList = tenantProperties.getAzureIotHubList();

            LoApiClient loApiClient = new LoApiClient(new RestTemplate(), liveObjectsProperties, getAuthenticationHeaders(liveObjectsProperties.getApiKey()));

            azureIotHubList.forEach(azureIotHubProperties -> {
                try {
                    DeviceTwin deviceTwin = DeviceTwin.createFromConnectionString(azureIotHubProperties.getIotConnectionString());
                    RegistryManager registryManager = RegistryManager.createFromConnectionString(azureIotHubProperties.getIotConnectionString());
                    IoTDeviceProvider ioTDeviceProvider = new IoTDeviceProvider(deviceTwin, registryManager, azureIotHubProperties.getTagPlatformKey(), azureIotHubProperties.getTagPlatformValue());
                    IotClientCache iotClientCache = new IotClientCache();
                    LoCommandSender loCommandSender2 = new LoCommandSender(new RestTemplate(), getAuthenticationHeaders(liveObjectsProperties.getApiKey()), liveObjectsProperties);
                    IotHubAdapter iotHubAdapter = new IotHubAdapter(ioTDeviceProvider, loCommandSender2, messageSender, iotClientCache, azureIotHubProperties);

                    MessageProducerSupport mqttInbound = mqttInbound(azureIotHubProperties, liveObjectsProperties);
                    IntegrationFlow mqttInFlow = mqttInFlow(iotHubAdapter, mqttInbound);
                    integrationFlowContext.registration(mqttInFlow).id(azureIotHubProperties.getLoDevicesGroup()).register();

                    DeviceSynchronizationTask deviceSynchronizationTask = new DeviceSynchronizationTask(iotHubAdapter, mqttInbound, loApiClient, azureIotHubProperties);
                    taskScheduler.scheduleAtFixedRate(deviceSynchronizationTask, Duration.ofSeconds(liveObjectsProperties.getSynchronizationDeviceInterval()));

                } catch (IOException e) {
                    throw new InitializationException(e);
                }
            });

        });
    }

    private HttpHeaders getAuthenticationHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-API-KEY", apiKey);
        headers.set("X-Total-Count", "true");
        return headers;
    }

    public MqttPahoClientFactory mqttClientFactory(LiveObjectsProperties loProperties) {
        LOG.info("Connecting to mqtt server: {}", loProperties.getUri());
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[] { loProperties.getUri() });
        opts.setUserName(loProperties.getUsername());
        opts.setPassword(loProperties.getApiKey().toCharArray());
        opts.setKeepAliveInterval(loProperties.getKeepAliveIntervalSeconds());
        opts.setConnectionTimeout(loProperties.getConnectionTimeout());
        factory.setConnectionOptions(opts);
        return factory;
    }

    private IntegrationFlow mqttInFlow(IotHubAdapter iotHubAdapter, MessageProducerSupport messageProducerSupport) {
        return IntegrationFlows.from(messageProducerSupport).<String, String>route(this::getMesageType, mapping -> mapping.resolutionRequired(false).subFlowMapping(DATA_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
            counterProvider.evtReceived().increment();
            iotHubAdapter.sendMessage((Message<String>) msg);
        })).subFlowMapping(DEVICE_CREATED_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
            Optional<String> deviceId = getDeviceId((Message<String>) msg);
            if (deviceId.isPresent()) {
                iotHubAdapter.createDeviceClient(deviceId.get());
            }
        })).subFlowMapping(DEVICE_DELETED_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
            Optional<String> deviceId = getDeviceId((Message<String>) msg);
            if (deviceId.isPresent()) {
                iotHubAdapter.deleteDevice(deviceId.get());
            }
        })).defaultSubFlowMapping(subflow -> subflow.handle(msg -> LOG.error("Unknow message type of message: {}", msg)))).get();
    }

    private MessageProducerSupport mqttInbound(AzureIotHubProperties hubProperties, LiveObjectsProperties loProperties) {
        LOG.info("Connecting to mqtt topics: {},{}", hubProperties.getLoMessagesTopic(), hubProperties.getLoDevicesTopic());
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(loProperties.getClientId() + UUID.randomUUID(), mqttClientFactory(loProperties), "fifo/" + hubProperties.getLoMessagesTopic(), "fifo/" + hubProperties.getLoDevicesTopic());

        adapter.setAutoStartup(false);
        adapter.setRecoveryInterval(loProperties.getRecoveryInterval());
        adapter.setCompletionTimeout(loProperties.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(loProperties.getQos());
        return adapter;
    }

    private Optional<String> getDeviceId(Message<String> msg) {
        String id = null;
        try {
            id = new JSONObject(msg.getPayload()).getString(DEVICE_ID_FIELD);
        } catch (JSONException e) {
            LOG.error("No device id in payload");
        }
        return Optional.ofNullable(id);
    }

    private String getMesageType(String msg) {
        try {
            return new JSONObject(msg).getString(TYPE_FIELD);
        } catch (JSONException e) {
            LOG.error("No message type in payload");
            return UNKNOW_MESSAGE_TYPE;
        }
    }
}