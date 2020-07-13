/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.exceptions.CommandException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LoCommandSender {
    private static final String COMMAND_URL_PATH = "/v1/deviceMgt/devices/%s/commands?validate=true";

    private LoProperties loProperties;
    private RestTemplate restTemplate;
    private HttpHeaders authenticationHeaders;

    @Autowired
    public LoCommandSender(RestTemplate restTemplate, HttpHeaders authenticationHeaders, LoProperties loProperties) {
        this.restTemplate = restTemplate;
        this.authenticationHeaders = authenticationHeaders;
        this.loProperties = loProperties;
    }

    public void send(String deviceId, String command) {
        String url = String.format(loProperties.getApiUrl() + COMMAND_URL_PATH, deviceId);
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<String>(command, authenticationHeaders), Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new CommandException("Returned status " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }
}
