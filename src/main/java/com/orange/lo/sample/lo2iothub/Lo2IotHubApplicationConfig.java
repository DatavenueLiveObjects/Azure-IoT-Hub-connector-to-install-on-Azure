/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.MessageSender;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
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

    private MessageProducerSupport mqttInbound;
    private MessageSender messageSender;
    private Counters counterProvider;
    private IotDeviceManagement iotDeviceManagement; 

    @Autowired
    public Lo2IotHubApplicationConfig(MessageProducerSupport mqttInbound, MessageSender messageSender, Counters counterProvider, IotDeviceManagement iotDeviceManagement) {
        this.mqttInbound = mqttInbound;
        this.messageSender = messageSender;
        this.counterProvider = counterProvider;
        this.iotDeviceManagement = iotDeviceManagement;
    }

    @Bean
    public IntegrationFlow mqttInFlow() {
        return IntegrationFlows.from(mqttInbound)
            .<String, String> route(msg -> getMesageType(msg),
                mapping -> mapping
                    .resolutionRequired(false)
                    .subFlowMapping(DATA_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
                        counterProvider.evtReceived().increment();
                        messageSender.send((Message<String>) msg);
                    }))
                    .subFlowMapping(DEVICE_CREATED_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
                        Optional<String> deviceId = getDeviceId((Message<String>)msg);
                        if (deviceId.isPresent()) {
                            iotDeviceManagement.createDeviceClient(deviceId.get());
                        }
                    }))
                    .subFlowMapping(DEVICE_DELETED_MESSAGE_TYPE, subflow -> subflow.handle(msg -> {
                        Optional<String> deviceId = getDeviceId((Message<String>)msg);
                        if (deviceId.isPresent()) {
                            iotDeviceManagement.deleteDevice(deviceId.get());
                        }
                    }))
                    .defaultSubFlowMapping(subflow -> subflow.handle(msg -> LOG.error("Unknow message type of message: {}", msg)))
            )
            .get();
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

    private String getMesageType(String msg)  {
        try {
            return new JSONObject(msg).getString(TYPE_FIELD);
        } catch (JSONException e) {
            LOG.error("No message type in payload");
            return UNKNOW_MESSAGE_TYPE;
        }
    }
}