/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import com.orange.lo.sample.lo2iothub.utils.Counters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private Counters counterProvider;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private MappingJackson2HttpMessageConverter springJacksonConverter;
    @Mock
    private ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;

    private ApplicationConfig applicationConfig;

    @BeforeEach
    void setUp() {
        applicationConfig = new ApplicationConfig(counterProvider, applicationProperties, springJacksonConverter);
    }

    @Test
    void shouldCreateTaskScheduler() {
        TaskScheduler taskScheduler = applicationConfig.taskScheduler();
        assertNotNull(taskScheduler);
    }
}