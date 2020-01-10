package com.orange.lo.sample.lo2iothub.lo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LoCommandSender {
	private static final String COMMAND_URL_PATTERN  = "https://liveobjects.orange-business.com/api/v1/deviceMgt/devices/%s/commands?validate=true";
	
	private RestTemplate restTemplate;
	private HttpHeaders authenticationHeaders;
	
	@Autowired
	public LoCommandSender(RestTemplate restTemplate, HttpHeaders authenticationHeaders) {
		this.restTemplate = restTemplate;
		this.authenticationHeaders = authenticationHeaders;
	}
	
	public void send(String deviceId, String command) {
		String url = String.format(COMMAND_URL_PATTERN, deviceId);
		restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<String>(command, authenticationHeaders), String.class);
	}
}
