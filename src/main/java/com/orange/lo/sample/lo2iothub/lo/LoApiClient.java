/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.lo.model.ActionPolicy;
import com.orange.lo.sample.lo2iothub.lo.model.Actions;
import com.orange.lo.sample.lo2iothub.lo.model.DataMessage;
import com.orange.lo.sample.lo2iothub.lo.model.DeviceCreated;
import com.orange.lo.sample.lo2iothub.lo.model.DeviceDeleted;
import com.orange.lo.sample.lo2iothub.lo.model.FifoPublish;
import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import com.orange.lo.sample.lo2iothub.lo.model.LoQueue;
import com.orange.lo.sample.lo2iothub.lo.model.Triggers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class LoApiClient {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String X_RATELIMIT_REMAINING_HEADER = "X-Ratelimit-Remaining";
    private static final String X_RATELIMIT_RESET_HEADER = "X-Ratelimit-Reset";

    private static final String DEVICES_ENDPOINT = "/v1/deviceMgt/devices";
    private static final String TOPICS_ENDPOINT = "/v0/topics/fifo/";
    private static final String ACTION_POLICIES_ENDPOINT = "/v1/event2action/actionPolicies";

    private static final String DEVICE_ACTION_POLICY_NAME = "lo2iot-device-action-policy";
    private static final String MESSAGE_ACTION_POLICY_NAME = "lo2iot-message-action-policy";

    private final String DEVICES_PAGED_URL_TEMPLATE;

    private RestTemplate restTemplate;
    private LoProperties loProperties;
    private HttpHeaders authenticationHeaders;

    @Autowired
    public LoApiClient(RestTemplate restTemplate, LoProperties loProperties, HttpHeaders authenticationHeaders) {
        this.restTemplate = restTemplate;
        this.loProperties = loProperties;
        this.authenticationHeaders = authenticationHeaders;
        this.DEVICES_PAGED_URL_TEMPLATE = loProperties.getApiUrl() + DEVICES_ENDPOINT + "?limit=" + loProperties.getPageSize() + "&offset=%d&fields=id,name,group";
    }

    public List<LoDevice> getDevices() {
        List<LoDevice> devices = new ArrayList<>(loProperties.getPageSize());
        for (int offset = 0;; offset++) {
            LOG.trace("Calling LO url {}", getPagedDevicesUrl(offset));
            ResponseEntity<LoDevice[]> response = restTemplate.exchange(getPagedDevicesUrl(offset), HttpMethod.GET, new HttpEntity<>(authenticationHeaders), LoDevice[].class);
            LOG.trace("Got {} devices", response.getBody().length);
            if (response.getBody().length == 0) {
                break;
            }
            devices.addAll(Arrays.asList(response.getBody()));
            if (devices.size() >= Integer.parseInt(response.getHeaders().get(X_TOTAL_COUNT_HEADER).get(0))) {
                break;
            }
            if (Integer.parseInt(response.getHeaders().get(X_RATELIMIT_REMAINING_HEADER).get(0)) == 0) {
                long reset = Long.parseLong(response.getHeaders().get(X_RATELIMIT_RESET_HEADER).get(0));
                long current = System.currentTimeMillis();
                try {
                    Thread.sleep(reset - current);
                } catch (InterruptedException e) {
                    // no matter
                }
            }
        }
        LOG.trace("Devices: " + devices.toString());
        return devices;
    }

    public boolean messagesQueueExists() {
        return queueExists(loProperties.getMessagesTopic());
    }

    public boolean devicesQueueExists() {
        return queueExists(loProperties.getDevicesTopic());
    }

    private boolean queueExists(String queueName) {
        try {
            String url = loProperties.getApiUrl() + TOPICS_ENDPOINT + queueName;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authenticationHeaders), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
        } catch (RestClientResponseException e) {
            // no matter
        }
        return false;
    }

    public void createDevicesQueue() {
        createQueue(loProperties.getDevicesTopic());
    }

    public void createMessagesQueue() {
        createQueue(loProperties.getMessagesTopic());
    }

    private void createQueue(String queueName) {
        LoQueue loQueue = new LoQueue(queueName);
        HttpEntity<LoQueue> httpEntity = new HttpEntity<LoQueue>(loQueue, authenticationHeaders);
        restTemplate.exchange(loProperties.getApiUrl() + TOPICS_ENDPOINT, HttpMethod.POST, httpEntity, Void.class);
    }

    public boolean messageActionPolicyExists() {
        try {
            ResponseEntity<ActionPolicy[]> response = restTemplate.exchange(loProperties.getApiUrl() + ACTION_POLICIES_ENDPOINT + "?triggerType=dataMessage", HttpMethod.GET, new HttpEntity<>(authenticationHeaders), ActionPolicy[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                long count = Arrays.stream(response.getBody()).filter(ap -> ap.getTriggers().getDataMessage() != null).filter(ap -> ap.getActions().getFifoPublish() != null).filter(ap -> ap.getActions().getFifoPublish().stream().anyMatch(fp -> fp.getFifoName().equals(loProperties.getMessagesTopic()))).filter(ap -> ap.getEnabled()).count();

                return count > 0 ? true : false;
            }
        } catch (RestClientResponseException e) {
            // no matter
        }
        return false;
    }

    public boolean deviceActionPolicyExists() {
        try {
            ResponseEntity<ActionPolicy[]> response = restTemplate.exchange(loProperties.getApiUrl() + ACTION_POLICIES_ENDPOINT + "?triggerType=deviceCreated", HttpMethod.GET, new HttpEntity<>(authenticationHeaders), ActionPolicy[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                long count = Arrays.stream(response.getBody()).filter(ap -> ap.getTriggers().getDeviceCreated() != null).filter(ap -> ap.getTriggers().getDeviceDeleted() != null).filter(ap -> ap.getActions().getFifoPublish() != null).filter(ap -> ap.getActions().getFifoPublish().stream().anyMatch(fp -> fp.getFifoName().equals(loProperties.getDevicesTopic()))).filter(ap -> ap.getEnabled()).count();

                return count > 0 ? true : false;
            }
        } catch (RestClientResponseException e) {
            // no matter
        }
        return false;
    }

    public void createDeviceActionPolicy() {
        ActionPolicy actionPolicy = new ActionPolicy();
        actionPolicy.setEnabled(true);
        actionPolicy.setName(DEVICE_ACTION_POLICY_NAME);

        Triggers trigers = new Triggers(new DeviceCreated(), new DeviceDeleted());
        actionPolicy.setTriggers(trigers);

        Actions actions = new Actions();
        actions.addFifoPublish(new FifoPublish(loProperties.getDevicesTopic()));
        actionPolicy.setActions(actions);

        HttpEntity<ActionPolicy> httpEntity = new HttpEntity<ActionPolicy>(actionPolicy, authenticationHeaders);

        restTemplate.exchange(loProperties.getApiUrl() + ACTION_POLICIES_ENDPOINT, HttpMethod.POST, httpEntity, Void.class);
    }

    public void createMessageActionPolicy() {
        ActionPolicy actionPolicy = new ActionPolicy();
        actionPolicy.setEnabled(true);
        actionPolicy.setName(MESSAGE_ACTION_POLICY_NAME);

        Triggers trigers = new Triggers(new DataMessage());
        actionPolicy.setTriggers(trigers);

        Actions actions = new Actions();
        actions.addFifoPublish(new FifoPublish(loProperties.getMessagesTopic()));
        actionPolicy.setActions(actions);

        HttpEntity<ActionPolicy> httpEntity = new HttpEntity<ActionPolicy>(actionPolicy, authenticationHeaders);

        restTemplate.exchange(loProperties.getApiUrl() + ACTION_POLICIES_ENDPOINT, HttpMethod.POST, httpEntity, Void.class);
    }

    private String getPagedDevicesUrl(int offset) {
        return String.format(DEVICES_PAGED_URL_TEMPLATE, offset * loProperties.getPageSize());
    }
}
