/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.IoTDevice;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sdk.rest.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DeviceSynchronizationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IotHubAdapter iotHubAdapter;
    private final LoAdapter loAdapter;
    private final AzureIotHubProperties azureIotHubProperties;

    public DeviceSynchronizationTask(IotHubAdapter iotHubAdapter, LoAdapter loAdapter, AzureIotHubProperties azureIotHubProperties) {
        this.iotHubAdapter = iotHubAdapter;
        this.loAdapter = loAdapter;
        this.azureIotHubProperties = azureIotHubProperties;
    }

    @Override
    public void run() {

        LOG.debug("Synchronizing devices for group {}", azureIotHubProperties.getLoDevicesGroup());
        try {

            Set<String> loIds = loAdapter.getDevices(azureIotHubProperties.getLoDevicesGroup()).stream()
                    .map(Device::getId)
                    .collect(Collectors.toSet());
            if (!loIds.isEmpty()) {
                int poolSize = azureIotHubProperties.getSynchronizationThreadPoolSize();
                ThreadPoolExecutor synchronizingExecutor = new ThreadPoolExecutor(poolSize, poolSize, 10,
                        TimeUnit.SECONDS, new ArrayBlockingQueue<>(loIds.size()));
                List<Callable<Void>> collect = loIds.stream().map(id -> (Callable<Void>) () -> {
                    iotHubAdapter.createDeviceClient(id);
                    return null;
                }).collect(Collectors.toList());
                synchronizingExecutor.invokeAll(collect);
            }
            Set<String> iotIds = iotHubAdapter.getDevices().stream()
                    .map(IoTDevice::getId)
                    .collect(Collectors.toSet());
            iotIds.removeAll(loIds);
            iotIds.forEach(id -> {
                LOG.debug("remove from cache and iot device {}", id);
                iotHubAdapter.deleteDevice(id);
            });

        } catch (Exception e) {
            LOG.error("Error while synchronizing devices", e);
        }

        loAdapter.startListeningForMessages();
    }

}