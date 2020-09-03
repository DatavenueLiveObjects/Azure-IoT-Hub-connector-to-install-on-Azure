/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.exceptions.CommandException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class LoCommandSender {
    private static final String COMMAND_URL_PATH = "/v1/deviceMgt/devices/%s/commands?validate=true";

    private LiveObjectsProperties loProperties;
    private RestTemplate restTemplate;
    private HttpHeaders authenticationHeaders;

    public LoCommandSender(RestTemplate restTemplate, HttpHeaders authenticationHeaders, LiveObjectsProperties loProperties) {
        this.restTemplate = restTemplate;
        this.authenticationHeaders = authenticationHeaders;
        this.loProperties = loProperties;
    }

    public void send(String deviceId, String command) {
        String s = loProperties.getApiUrl() + COMMAND_URL_PATH;
        String url = String.format(s, deviceId);
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
