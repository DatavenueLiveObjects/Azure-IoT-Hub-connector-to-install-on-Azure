package com.orange.lo.sample.lo2iothub.azure;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;

@Configuration
public class AzureConfiguration {
	
	AzureProperties azureProperties;
	
	public AzureConfiguration(AzureProperties azureProperties) {
		this.azureProperties = azureProperties;
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	public DeviceTwin deviceTwin() throws IOException {
		return DeviceTwin.createFromConnectionString(azureProperties.getIotConnectionString());
	}
	
	@Bean
	public RegistryManager registryManager() throws IOException {
		return RegistryManager.createFromConnectionString(azureProperties.getIotConnectionString());
	}	
}
