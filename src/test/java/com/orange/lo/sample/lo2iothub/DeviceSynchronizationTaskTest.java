package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.IoTDevice;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoApiClient;
import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.endpoint.MessageProducerSupport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSynchronizationTaskTest {

    private static final String LO_DEVICES_GROUP = "device-group-lo";
    private static final int SYNCHRONIZATION_POOL_SIZE = 1;
    public static final List<LoDevice> LO_DEVICES = Arrays.asList(
            new LoDevice("lo-device-id-01", "lo-device-name-01"),
            new LoDevice("lo-device-id-02", "lo-device-name-02")
    );
    public static final List<IoTDevice> IOT_DEVICES = Collections.singletonList(
            new IoTDevice("lo-device-id-03")
    );

    @Mock
    private IotHubAdapter iotHubAdapter;
    @Mock
    private MessageProducerSupport messageProducerSupport;
    @Mock
    private LoApiClient loApiClient;
    private DeviceSynchronizationTask deviceSynchronizationTask;

    @BeforeEach
    void setUp() {
        when(loApiClient.getDevices(LO_DEVICES_GROUP)).thenReturn(LO_DEVICES);
        when(iotHubAdapter.getDevices()).thenReturn(IOT_DEVICES);

        AzureIotHubProperties azureIotHubProperties = new AzureIotHubProperties();
        azureIotHubProperties.setLoDevicesGroup(LO_DEVICES_GROUP);
        azureIotHubProperties.setSynchronizationThreadPoolSize(SYNCHRONIZATION_POOL_SIZE);
        deviceSynchronizationTask = new DeviceSynchronizationTask(iotHubAdapter, messageProducerSupport, loApiClient, azureIotHubProperties);
    }

    @Test
    void shouldSynchronizeDevicesBetweenLApiClientAndIotHubAdapter() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(deviceSynchronizationTask);
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(loApiClient, times(1)).getDevices(eq(LO_DEVICES_GROUP));
        verify(iotHubAdapter, times(2)).createDeviceClient(anyString());
        verify(iotHubAdapter, times(1)).getDevices();
        verify(iotHubAdapter, times(1)).deleteDevice(anyString());
        verify(messageProducerSupport, times(1)).start();
    }
}