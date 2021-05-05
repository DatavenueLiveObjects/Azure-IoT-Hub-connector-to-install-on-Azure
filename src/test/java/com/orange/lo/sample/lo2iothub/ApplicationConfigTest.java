package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.MessageSender;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private Counters counterProvider;
    @Mock
    private MessageSender messageSender;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private MappingJackson2HttpMessageConverter springJacksonConverter;

    private ApplicationConfig applicationConfig;

    @BeforeEach
    void setUp() {
        applicationConfig = new ApplicationConfig(counterProvider, messageSender,
                applicationProperties, springJacksonConverter);
    }

    @Test
    void shouldCreateTaskScheduler() {
        TaskScheduler taskScheduler = applicationConfig.taskScheduler();
        assertNotNull(taskScheduler);
    }
}