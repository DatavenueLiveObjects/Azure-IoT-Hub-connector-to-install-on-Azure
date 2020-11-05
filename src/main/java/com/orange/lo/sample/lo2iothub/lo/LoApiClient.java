/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import com.orange.lo.sample.lo2iothub.lo.model.LoGroup;

import java.lang.invoke.MethodHandles;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class LoApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_GROUP_ID = "root";
    private static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String X_RATELIMIT_REMAINING_HEADER = "X-Ratelimit-Remaining";
    private static final String X_RATELIMIT_RESET_HEADER = "X-Ratelimit-Reset";

    private static final String DEVICES_ENDPOINT = "/v1/deviceMgt/devices";
    private static final String GROUPS_ENDPOINT = "/v1/deviceMgt/groups";

    private final String devicesPagedUrlTemplate;
    private final String groupsPagedUrlTemplate;

    private RestTemplate restTemplate;
    private LiveObjectsProperties loProperties;
    private HttpHeaders authenticationHeaders;

    private Map<String, String> groupsMap = new HashMap<>();

    public LoApiClient(RestTemplate restTemplate, LiveObjectsProperties loProperties,
                       HttpHeaders authenticationHeaders) {
        this.restTemplate = restTemplate;
        this.loProperties = loProperties;
        this.authenticationHeaders = authenticationHeaders;
        this.devicesPagedUrlTemplate = getDevicesPagedUrlTemplate(loProperties);
        this.groupsPagedUrlTemplate = getGroupsPagedUrlTemplate(loProperties);
        initialize();
    }

    private String getDevicesPagedUrlTemplate(LiveObjectsProperties loProperties) {
        String apiUrl = loProperties.getApiUrl();
        int pageSize = loProperties.getPageSize();
        return apiUrl + DEVICES_ENDPOINT + "?limit=" + pageSize + "&offset=%d&groupId=%s&fields=id,name,group";
    }

    private String getGroupsPagedUrlTemplate(LiveObjectsProperties loProperties) {
        String apiUrl = loProperties.getApiUrl();
        int pageSize = loProperties.getPageSize();
        return apiUrl + GROUPS_ENDPOINT + "?limit=" + pageSize + "&offset=" + "%d";
    }

    private void initialize() {
        LOG.info("Managing groups of devices");

        try {
            getExistingGroups();
        } catch (Exception e) {
            LOG.error("Unexpected error while managing group: {}", e.getMessage());
            System.exit(1);
        }
    }

    private void getExistingGroups() {
        HttpEntity<Void> httpEntity = new HttpEntity<>(authenticationHeaders);
        ArrayList<LoGroup> emptyList = new ArrayList<>();

        LOG.debug("Trying to get existing groups");
        int retrievedGroups = 0;
        for (int offset = 0; ; offset++) {
            try {
                String pagedGroupsUrl = getPagedGroupsUrl(offset);
                ResponseEntity<LoGroup[]> response =
                        restTemplate.exchange(pagedGroupsUrl, HttpMethod.GET, httpEntity, LoGroup[].class);
                List<LoGroup> loGroups = Optional.ofNullable(response.getBody())
                        .map(Arrays::asList)
                        .orElse(emptyList);

                retrievedGroups += loGroups.size();
                loGroups.forEach(g -> groupsMap.put(g.getPathNode(), g.getId()));

                if (loGroups.isEmpty() || retrievedGroups >= getTotalCount(response)) {
                    break;
                }
            } catch (HttpClientErrorException e) {
                LOG.error("Cannot retrieve information about groups \n {}", e.getResponseBodyAsString());
                System.exit(1);
            }
        }
    }

    public List<LoDevice> getDevices(String groupName) {
        List<LoDevice> devices = new ArrayList<>(loProperties.getPageSize());
        HttpEntity<Object> requestEntity = new HttpEntity<>(authenticationHeaders);
        ArrayList<LoDevice> emptyList = new ArrayList<>();

        for (int offset = 0; ; offset++) {
            String pagedDevicesUrl = getPagedDevicesUrl(offset, groupName);
            LOG.trace("Calling LO url {}", pagedDevicesUrl);
            ResponseEntity<LoDevice[]> response =
                    restTemplate.exchange(pagedDevicesUrl, HttpMethod.GET, requestEntity, LoDevice[].class);
            List<LoDevice> loDevices = Optional.ofNullable(response.getBody())
                    .map(Arrays::asList)
                    .orElse(emptyList);

            LOG.trace("Got {} devices", loDevices.size());
            devices.addAll(loDevices);
            if (loDevices.isEmpty() || devices.size() >= getTotalCount(response)) {
                break;
            }
            if (Integer.parseInt(getHeaderValue(response, X_RATELIMIT_REMAINING_HEADER)) == 0) {
                long reset = Long.parseLong(getHeaderValue(response, X_RATELIMIT_RESET_HEADER));
                long current = System.currentTimeMillis();
                try {
                    Thread.sleep(reset - current);
                } catch (Exception e) {
                    LOG.error("Exception while getting devices: {}", e.getMessage());
                }
            }
        }
        LOG.trace("Devices: {}", devices);
        return devices;
    }

    private String getPagedDevicesUrl(int offset, String groupName) {
        String groupsMapOrDefault = groupsMap.getOrDefault(groupName, DEFAULT_GROUP_ID);
        return String.format(devicesPagedUrlTemplate, offset * loProperties.getPageSize(), groupsMapOrDefault);
    }

    private String getPagedGroupsUrl(int offset) {
        return String.format(groupsPagedUrlTemplate, offset * loProperties.getPageSize());
    }

    private static int getTotalCount(ResponseEntity<?> response) {
        String headerValue = getHeaderValue(response, X_TOTAL_COUNT_HEADER);
        return Integer.parseInt(headerValue);
    }

    private static String getHeaderValue(ResponseEntity<?> response, String xTotalCountHeader) {
        return response.getHeaders().get(xTotalCountHeader).get(0);
    }
}
