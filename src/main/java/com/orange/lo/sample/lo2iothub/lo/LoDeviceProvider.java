package com.orange.lo.sample.lo2iothub.lo;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LoDeviceProvider {
	
	private RestTemplate restTemplate;	
	private LoProperties loProperties;
	private HttpEntity<Void> entity;	
	
	@Autowired
	public LoDeviceProvider(RestTemplate restTemplate, LoProperties loProperties, HttpHeaders authenticationHeaders) {
		this.restTemplate = restTemplate;
		this.loProperties = loProperties;
		this.entity = new HttpEntity<Void>(authenticationHeaders);
	}
	
	public List<LoDevice> getDevices() {
        ResponseEntity<LoDevice[]> response = restTemplate.exchange(loProperties.getDeviceUrl(), HttpMethod.GET, entity, LoDevice[].class);        
		return Arrays.asList(response.getBody());
	}
}
