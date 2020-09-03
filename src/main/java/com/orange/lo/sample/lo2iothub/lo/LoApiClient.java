/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.LiveObjectsProperties;
import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import com.orange.lo.sample.lo2iothub.lo.model.LoGroup;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class LoApiClient {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_GROUP_ID = "root";
    private static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String X_RATELIMIT_REMAINING_HEADER = "X-Ratelimit-Remaining";
    private static final String X_RATELIMIT_RESET_HEADER = "X-Ratelimit-Reset";

    private static final String DEVICES_ENDPOINT = "/v1/deviceMgt/devices";
    private static final String GROOUPS_ENDPOINT = "/v1/deviceMgt/groups";

    private final String DEVICES_PAGED_URL_TEMPLATE;
    private final String GROUPS_PAGED_URL_TEMPLATE;

    private RestTemplate restTemplate;
    private LiveObjectsProperties loProperties;
    private HttpHeaders authenticationHeaders;

    private Map<String, String> groupsMap = new HashMap<String, String>();

    public LoApiClient(RestTemplate restTemplate, LiveObjectsProperties loProperties, HttpHeaders authenticationHeaders) {
        this.restTemplate = restTemplate;
        this.loProperties = loProperties;
        this.authenticationHeaders = authenticationHeaders;
        this.DEVICES_PAGED_URL_TEMPLATE = loProperties.getApiUrl() + DEVICES_ENDPOINT + "?limit=" + loProperties.getPageSize() + "&offset=%d&groupId=%s&fields=id,name,group";
        this.GROUPS_PAGED_URL_TEMPLATE = loProperties.getApiUrl() + GROOUPS_ENDPOINT + "?limit=" + loProperties.getPageSize() + "&offset=" + "%d";
        initialize();
    }

    private void initialize() {
        LOG.info("Managing groups of devices");

        try {
            HttpEntity<Void> httpEntity = new HttpEntity<Void>(authenticationHeaders);

            LOG.debug("Trying to get existing groups");
            int retrievedGroups = 0;
            for (int offset = 0;; offset++) {
                try {
                    ResponseEntity<LoGroup[]> response = restTemplate.exchange(getPagedGroupsUrl(offset), HttpMethod.GET, httpEntity, LoGroup[].class);
                    if (response.getBody().length == 0) {
                        break;
                    }
                    retrievedGroups += response.getBody().length;

                    Arrays.stream(response.getBody()).forEach(g -> groupsMap.put(g.getPathNode(), g.getId()));

                    if (retrievedGroups >= Integer.parseInt(response.getHeaders().get(X_TOTAL_COUNT_HEADER).get(0))) {
                        break;
                    }
                } catch (HttpClientErrorException e) {
                    LOG.error("Cannot retrieve information about groups \n {}", e.getResponseBodyAsString());
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected error while managing group {}", e.getMessage());
            System.exit(1);
        }
    }

    public List<LoDevice> getDevices(String groupName) {
        List<LoDevice> devices = new ArrayList<>(loProperties.getPageSize());
        for (int offset = 0;; offset++) {
            LOG.trace("Calling LO url {}", getPagedDevicesUrl(offset, groupName));
            ResponseEntity<LoDevice[]> response = restTemplate.exchange(getPagedDevicesUrl(offset, groupName), HttpMethod.GET, new HttpEntity<>(authenticationHeaders), LoDevice[].class);
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

    private String getPagedDevicesUrl(int offset, String groupName) {
        return String.format(DEVICES_PAGED_URL_TEMPLATE, offset * loProperties.getPageSize(), groupsMap.getOrDefault(groupName, DEFAULT_GROUP_ID));
    }

    private String getPagedGroupsUrl(int offset) {
        return String.format(GROUPS_PAGED_URL_TEMPLATE, offset * loProperties.getPageSize());
    }
}
