/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDevice;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoApiClient;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.endpoint.MessageProducerSupport;

public class DeviceSynchronizationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IotHubAdapter iotHubAdapter;
    private MessageProducerSupport messageProducerSupport;
    private LoApiClient loApiClient;
    private AzureIotHubProperties azureIotHubProperties;

    public DeviceSynchronizationTask(IotHubAdapter iotHubAdapter, MessageProducerSupport messageProducerSupport,
                                     LoApiClient loApiClient, AzureIotHubProperties azureIotHubProperties) {
        this.iotHubAdapter = iotHubAdapter;
        this.messageProducerSupport = messageProducerSupport;
        this.loApiClient = loApiClient;
        this.azureIotHubProperties = azureIotHubProperties;
    }

    @Override
    public void run() {

        LOG.debug("Synchronizing devices for group {}", azureIotHubProperties.getLoDevicesGroup());
        try {

            Set<String> loIds = loApiClient.getDevices(azureIotHubProperties.getLoDevicesGroup()).stream()
                    .map(loDevice -> StringEscapeUtils.escapeJava(loDevice.getId()))
                    .collect(Collectors.toSet());
            if (!loIds.isEmpty()) {
                int poolSize = azureIotHubProperties.getSynchronizationThreadPoolSize();
                ThreadPoolExecutor synchronizingExecutor = new ThreadPoolExecutor(poolSize, poolSize, 10,
                        TimeUnit.SECONDS, new ArrayBlockingQueue<>(loIds.size()));
                for (String deviceId : loIds) {
                    synchronizingExecutor.execute(() ->
                            iotHubAdapter.createDeviceClient(deviceId)
                    );
                }
                int synchronizationTimeout = calculateSynchronizationTimeout(loIds.size(), poolSize);
                synchronizingExecutor.shutdown();
                synchronizingExecutor.awaitTermination(synchronizationTimeout, TimeUnit.SECONDS);
            }
            Set<String> iotIds = iotHubAdapter.getDevices().stream()
                    .map(ioTDevice -> StringEscapeUtils.escapeJava(ioTDevice.getId()))
                    .collect(Collectors.toSet());
            iotIds.removeAll(loIds);
            iotIds.forEach(id -> {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("remove from cache and iot device {}", StringEscapeUtils.escapeJava(id));
                }
                iotHubAdapter.deleteDevice(id);
            });

        } catch (Exception e) {
            LOG.error("Error while synchronizing devices", e);
        }

        messageProducerSupport.start();
    }

    private static int calculateSynchronizationTimeout(int devices, int threadPoolSize) {
        return (devices / threadPoolSize + 1) * 5; // max 5 seconds for 1 operation
    }

}