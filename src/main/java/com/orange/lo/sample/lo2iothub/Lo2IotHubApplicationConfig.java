package com.orange.lo.sample.lo2iothub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;

import com.orange.lo.sample.lo2iothub.azure.MessageSender;

@EnableIntegration
@Configuration
public class Lo2IotHubApplicationConfig {

	private MessageProducerSupport mqttInbound;
	private MessageSender messageSender;
	private Counters counterProvider;
	
	@Autowired
	public Lo2IotHubApplicationConfig(MessageProducerSupport mqttInbound, MessageSender messageSender, Counters counterProvider) {
		this.mqttInbound = mqttInbound;
		this.messageSender = messageSender;
		this.counterProvider = counterProvider;
	}
	
	@Bean
    public IntegrationFlow mqttInFlow() {
        return IntegrationFlows.from(mqttInbound)                
        		.handle(msg -> {
        				counterProvider.evtReceived().increment();
        				messageSender.send((Message<String>) msg);
        			}
        		)        		
                .get();
    }
}
