/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.azure.IoTDeviceProvider;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LoConfig {

    private static final Logger LOG = LoggerFactory.getLogger(IoTDeviceProvider.class);

    private LoProperties loProperties;

    public LoConfig(LoProperties loProperties) {
        this.loProperties = loProperties;
    }

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

    @Bean
    public MessageProducerSupport mqttInbound() {
        LOG.info("Connecting to mqtt topics: {},{}", loProperties.getMessagesTopic(), loProperties.getDevicesTopic());
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(loProperties.getClientId(), mqttClientFactory(), new String[] { "fifo/" + loProperties.getMessagesTopic(), "fifo/" + loProperties.getDevicesTopic() });

        adapter.setAutoStartup(false);
        adapter.setRecoveryInterval(loProperties.getRecoveryInterval());
        adapter.setCompletionTimeout(loProperties.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(loProperties.getQos());
        return adapter;
    }

    @Bean
    public HttpHeaders authenticationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-API-KEY", loProperties.getApiKey());
        headers.set("X-Total-Count", "true");
        return headers;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
