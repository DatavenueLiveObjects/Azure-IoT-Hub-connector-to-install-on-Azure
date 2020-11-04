package com.orange.lo.sample.lo2iothub.lo;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoCommandSenderTest {

    private static final String API_URL = "https://liveobjects.orange-business.com/api";

    private LiveObjectsProperties loProperties;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private HttpHeaders authenticationHeaders;
    @Mock
    private ResponseEntity<Void> responseEntity;

    private LoCommandSender loCommandSender;

    @BeforeEach
    void setUp() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(responseEntity);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        this.loProperties = new LiveObjectsProperties();
        this.loCommandSender = new LoCommandSender(restTemplate, authenticationHeaders, loProperties);
    }

    @Test
    void shouldSendCommandToCommandsEndpoint() {
        loProperties.setApiUrl(API_URL);
        String expectedUrl = loProperties.getApiUrl() + "/v1/deviceMgt/devices/device-Id/commands?validate=true";

        loCommandSender.send("device-Id", "command");

        verify(restTemplate, times(1)).exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }
}