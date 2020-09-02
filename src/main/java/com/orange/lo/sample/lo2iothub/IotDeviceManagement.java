/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.AzureProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDevice;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoApiClient;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.orange.lo.sample.lo2iothub.lo.model.LoDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class IotDeviceManagement {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Map<String, IotHubAdapter> iotHubAdapterMap;
    private Map<String, MessageProducerSupport> messageProducerSupportMap;
    private LoApiClient loApiClient;
    private AzureProperties azureProperties;

    public IotDeviceManagement(Map<String, IotHubAdapter> iotHubAdapterMap, Map<String, MessageProducerSupport> messageProducerSupportMap, LoApiClient loApiClient, AzureProperties azureProperties) {
        this.iotHubAdapterMap = iotHubAdapterMap;
        this.messageProducerSupportMap = messageProducerSupportMap;
        this.loApiClient = loApiClient;
        this.azureProperties = azureProperties;
    }

    @Scheduled(fixedRateString = "${lo.synchronization-device-interval}")
    public void synchronizeDevices() {
        azureProperties.getAzureIotHubList().forEach(hubProperties -> {
            LOG.debug("Synchronizing devices for group {}", hubProperties.getLoDevicesGroup());
            try {
                IotHubAdapter iotHubAdapter = iotHubAdapterMap.get(hubProperties.getLoDevicesGroup());
                Set<String> loIds = loApiClient.getDevices(hubProperties.getLoDevicesGroup()).stream().map(LoDevice::getId).collect(Collectors.toSet());
                if (!loIds.isEmpty()) {
                    ThreadPoolExecutor synchronizingExecutor = new ThreadPoolExecutor(hubProperties.getSynchronizationThreadPoolSize(), hubProperties.getSynchronizationThreadPoolSize(), 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(loIds.size()));
                    for (String deviceId : loIds) {
                        synchronizingExecutor.execute(() ->
                                iotHubAdapter.createDeviceClient(deviceId)
                        );
                    }
                    int synchronizationTimeout = calculateSynchronizationTimeout(loIds.size(), hubProperties.getSynchronizationThreadPoolSize());
                    synchronizingExecutor.shutdown();
                    synchronizingExecutor.awaitTermination(synchronizationTimeout, TimeUnit.SECONDS);
                }
                Set<String> iotIds = iotHubAdapter.getDevices().stream().map(IoTDevice::getId).collect(Collectors.toSet());
                iotIds.removeAll(loIds);
                iotIds.forEach(id -> {
                    LOG.debug("remove from cache and iot device {}", id);
                    iotHubAdapter.deleteDevice(id);
                });

            } catch (Exception e) {
                LOG.error("Error while synchronizing devices", e);
            }
        });

        messageProducerSupportMap.values().forEach(AbstractEndpoint::start);
    }

    private int calculateSynchronizationTimeout(int devices, int threadPoolSize) {
        return (devices / threadPoolSize + 1) * 5; // max 5 seconds for 1 operation
    }
}