/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub;

import com.orange.lo.sample.lo2iothub.azure.AzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.azure.IotDeviceId;
import com.orange.lo.sample.lo2iothub.azure.IotHubAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sdk.rest.model.Device;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSynchronizationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IotHubAdapter iotHubAdapter;
    private final LoAdapter loAdapter;
    private final AzureIotHubProperties azureIotHubProperties;
    private final boolean deviceSynchronization;

    public DeviceSynchronizationTask(IotHubAdapter iotHubAdapter, LoAdapter loAdapter,
                                     AzureIotHubProperties azureIotHubProperties, boolean deviceSynchronization) {
        this.iotHubAdapter = iotHubAdapter;
        this.loAdapter = loAdapter;
        this.azureIotHubProperties = azureIotHubProperties;
        this.deviceSynchronization = deviceSynchronization;
    }

    @Override
    public void run() {
        try {
            if (isDeviceSynchronizationEnabled()) {
                LOG.debug("Synchronizing devices for group {}", azureIotHubProperties.getLoDevicesGroup());
                createDeviceClientsAndSynchronizeDevicesFromLOToIoTHub();
            } else {
                LOG.debug("Synchronizing device clients");
                createOnlyDeviceClientsForDevicesExistingInIoTHub();
            }
        } catch (Exception e) {
            LOG.error("Error while synchronizing devices/creating device clients", e);
        }
    }

    private boolean isDeviceSynchronizationEnabled() {
        return deviceSynchronization;
    }

    private void createDeviceClientsAndSynchronizeDevicesFromLOToIoTHub() throws InterruptedException {
        Set<String> loIds = getDeviceIDsFromLO(azureIotHubProperties.getLoDevicesGroup());
        if (!loIds.isEmpty()) {
            createOrGerDeviceClients(loIds);
        }
        Set<String> iotIds = getDeviceIDsFromIoTHub();
        iotIds.removeAll(loIds);
        iotIds.forEach(id -> {
            LOG.debug("remove from cache and iot device {}", id);
            iotHubAdapter.deleteDevice(id);
        });
    }

    @NotNull
    private Set<String> getDeviceIDsFromLO(String loDevicesGroup) {
        return loAdapter.getDevices(loDevicesGroup).stream()
                .map(Device::getId)
                .collect(Collectors.toSet());
    }

    private void createOrGerDeviceClients(Set<String> loIds) throws InterruptedException {
        int poolSize = azureIotHubProperties.getDeviceRegistrationThreadPoolSize();
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(loIds.size());
        ThreadPoolExecutor synchronizingExecutor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, workQueue);
        List<Callable<Void>> collect = loIds.stream().map(id -> (Callable<Void>) () -> {
            iotHubAdapter.createOrGetDeviceClientManager(id);
            return null;
        }).collect(Collectors.toList());
        synchronizingExecutor.invokeAll(collect);
        synchronizingExecutor.shutdown();
    }

    @NotNull
    private Set<String> getDeviceIDsFromIoTHub() {
        return iotHubAdapter.getIotDeviceIds().stream()
                .map(IotDeviceId::getId)
                .collect(Collectors.toSet());
    }

    private void createOnlyDeviceClientsForDevicesExistingInIoTHub() throws InterruptedException {
        Set<String> loIds = getDeviceIDsFromLO(null);
        Set<String> iotIds = getDeviceIDsFromIoTHub();
        LOG.info("Number of devices in LO: {}", loIds.size());
        LOG.info("Number of devices in IoT Hub: {}", iotIds.size());

        loIds.retainAll(iotIds);
        LOG.info("Number of devices from LO that exist in IoT Hub: {}", loIds.size());
        if (!loIds.isEmpty()) {
            createOrGerDeviceClients(loIds);
            iotHubAdapter.removeDeviceClientsForNonExistentDevices(loIds);
        }
    }
}