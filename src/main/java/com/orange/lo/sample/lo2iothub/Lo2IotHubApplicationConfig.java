/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.AzureProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;
import com.orange.lo.sample.lo2iothub.azure.IotClientCache;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.exceptions.InitializationException;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;
import com.orange.lo.sample.lo2iothub.lo.LoProperties;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.context.support.GenericApplicationContext;
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

@EnableIntegration
@Configuration
public class Lo2IotHubApplicationConfig {

    private static final String DEVICE_ID_FIELD = "deviceId";
    private static final String TYPE_FIELD = "type";
    private static final String UNKNOW_MESSAGE_TYPE = "unknow";
    private static final String DEVICE_DELETED_MESSAGE_TYPE = "deviceDeleted";
    private static final String DEVICE_CREATED_MESSAGE_TYPE = "deviceCreated";
    private static final String DATA_MESSAGE_TYPE = "dataMessage";

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Counters counterProvider;
    private IntegrationFlowContext integrationflowContext;
    private LoProperties loProperties;
    private AzureProperties azureProperties;
    private LoCommandSender loCommandSender;
    private MessageSender messageSender;

    @Autowired
    public Lo2IotHubApplicationConfig(Counters counterProvider, IntegrationFlowContext integrationflowContext, LoProperties loProperties, AzureProperties azureProperties, GenericApplicationContext genericApplicationContext, LoCommandSender loCommandSender, MessageSender messageSender) {
        this.counterProvider = counterProvider;
        this.integrationflowContext = integrationflowContext;
        this.loProperties = loProperties;
        this.azureProperties = azureProperties;
        this.loCommandSender = loCommandSender;
        this.messageSender = messageSender;

    }

    @Bean
    public Map<String, IotHubAdapter> iotHubAdapterMap() {
        Map<String, IotHubAdapter> map = new HashMap<String, IotHubAdapter>();

        azureProperties.getAzureIotHubList().forEach(hubProperties -> {
            try {
                DeviceTwin deviceTwin = DeviceTwin.createFromConnectionString(hubProperties.getIotConnectionString());
                RegistryManager registryManager = RegistryManager.createFromConnectionString(hubProperties.getIotConnectionString());
                IoTDeviceProvider ioTDeviceProvider = new IoTDeviceProvider(deviceTwin, registryManager, hubProperties.getTagPlatformKey(), hubProperties.getTagPlatformValue());
                IotClientCache iotClientCache = new IotClientCache();
                map.put(hubProperties.getLoDevicesGroup(), new IotHubAdapter(ioTDeviceProvider, loCommandSender, messageSender, iotClientCache, hubProperties));
            } catch (IOException e) {
                throw new InitializationException(e);
            }
        });
        return map;
    }

    @Bean
    public Map<String, MessageProducerSupport> messageProducerSupportMap() {
        Map<String, MessageProducerSupport> map = new HashMap<String, MessageProducerSupport>();
        azureProperties.getAzureIotHubList().forEach(hubProperties -> {
            map.put(hubProperties.getLoDevicesGroup(), mqttInbound(hubProperties));
        });
        return map;
    }

    @Bean
    public List<IntegrationFlow> integrationFlowList() {
        List<IntegrationFlow> list = new ArrayList<IntegrationFlow>();
        Map<String, IotHubAdapter> iotHubAdapterMap = iotHubAdapterMap();
        Map<String, MessageProducerSupport> messageProducerSupportMap = messageProducerSupportMap();

        azureProperties.getAzureIotHubList().forEach(hubProperties -> {
            IntegrationFlow mqttInFlow = mqttInFlow(iotHubAdapterMap.get(hubProperties.getLoDevicesGroup()), messageProducerSupportMap.get(hubProperties.getLoDevicesGroup()));
            list.add(mqttInFlow);
            integrationflowContext.registration(mqttInFlow).id(hubProperties.getLoDevicesGroup()).register();
        });
        return list;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
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
        return IntegrationFlows.from(messageProducerSupport).<String, String>route(msg -> getMesageType(msg), mapping -> mapping.resolutionRequired(false).subFlowMapping(DATA_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
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

    private MessageProducerSupport mqttInbound(AzureIotHubProperties hubProperties) {
        LOG.info("Connecting to mqtt topics: {},{}", hubProperties.getLoMessagesTopic(), hubProperties.getLoDevicesTopic());
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(loProperties.getClientId() + UUID.randomUUID(), mqttClientFactory(), new String[] { "fifo/" + hubProperties.getLoMessagesTopic(), "fifo/" + hubProperties.getLoDevicesTopic() });

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