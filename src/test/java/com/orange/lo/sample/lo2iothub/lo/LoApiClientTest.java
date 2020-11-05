package com.orange.lo.sample.lo2iothub.lo;

import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import com.orange.lo.sample.lo2iothub.lo.model.LoGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoApiClientTest {

    private static final String API_URL = "https://liveobjects.orange-business.com/api";
    private static final String GROUP_NAME = "root";
    private static final List<LoDevice> DEVICES = Arrays.asList(
            new LoDevice("device-one", "device one"),
            new LoDevice("device-two", "device two")
    );

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private HttpHeaders authenticationHeaders;
    private LoApiClient loApiClient;

    @BeforeEach
    void setUp() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Total-Count", String.valueOf(0));

        ResponseEntity<LoGroup[]> loGroupResponseEntity = ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new LoGroup[0]);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(LoGroup[].class)))
                .thenReturn(loGroupResponseEntity);

        ResponseEntity<LoDevice[]> devicesResponseEntity = ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(DEVICES.toArray(new LoDevice[0]));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(LoDevice[].class)))
                .thenReturn(devicesResponseEntity);

        LiveObjectsProperties loProperties = new LiveObjectsProperties();
        loProperties.setApiUrl(API_URL);
        this.loApiClient = new LoApiClient(restTemplate, loProperties, authenticationHeaders);
    }

    @Test
    void shouldCorrectlyCallRestTemplateWhenGettingDevicesData() {
        List<LoDevice> devices = loApiClient.getDevices(GROUP_NAME);

        assertEquals(2, devices.size());
        verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(LoDevice[].class));
        verify(restTemplate, times(1)).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(LoGroup[].class));
    }
}