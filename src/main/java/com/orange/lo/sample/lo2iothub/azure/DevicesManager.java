/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoAdapter;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.orange.lo.sample.lo2iothub.utils.Counters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicesManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final AzureIotHubProperties azureIotHubProperties;
    private List<MultiplexingClientManager> multiplexingClientManagerList;
    private LoCommandSender loCommandSender;

    private final IoTDeviceProvider ioTDeviceProvider;
    private Counters counterProvider;
    private LoAdapter loAdapter;

    public DevicesManager(AzureIotHubProperties azureIotHubProperties, IoTDeviceProvider ioTDeviceProvider, Counters counterProvider) throws IotHubClientException {
        this.azureIotHubProperties = azureIotHubProperties;
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.counterProvider = counterProvider;
        this.multiplexingClientManagerList = Collections.synchronizedList(new LinkedList<>());
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public void setLoAdapter(LoAdapter loAdapter) {
        this.loAdapter = loAdapter;
    }

    public synchronized boolean containsDeviceClient(String deviceClientId) {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.deviceExisted(deviceClientId)) {
                return true;
            }
        }
        return false;
    }

    public void createDeviceClient(Device device) {
        DeviceClientManager deviceClientManager = new DeviceClientManager(device, azureIotHubProperties, loCommandSender, loAdapter, ioTDeviceProvider, counterProvider);
        MultiplexingClientManager freeMultiplexingClientManager = getFreeMultiplexingClientManager();
        deviceClientManager.setMultiplexingClientManager(freeMultiplexingClientManager);
        freeMultiplexingClientManager.registerDeviceClientManager(deviceClientManager);
    }

    public synchronized DeviceClientManager getDeviceClientManager(String deviceClientId) {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.deviceExisted(deviceClientId)) {
                return multiplexingClientManager.getDeviceClientManager(deviceClientId);
            }
        }
        return null;
    }

    public synchronized void removeDeviceClient(String deviceClientId) throws InterruptedException, IotHubClientException, TimeoutException {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.deviceExisted(deviceClientId)) {
                multiplexingClientManager.unregisterDeviceClient(deviceClientId);
            }
        }
    }

    private synchronized MultiplexingClientManager getFreeMultiplexingClientManager() {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.hasSpace()) {
                multiplexingClientManager.makeReservation();
                return multiplexingClientManager;
            }
        }
        MultiplexingClientManager freeMultiplexingClientManager = new MultiplexingClientManager(azureIotHubProperties, counterProvider, ioTDeviceProvider);
        freeMultiplexingClientManager.makeReservation();
        multiplexingClientManagerList.add(freeMultiplexingClientManager);
        return freeMultiplexingClientManager;
    }

    public void keepDeviceClientsOnlyForTheseDevices(Set<String> idsOfDeviceForWhichClientsShouldBeKept) {
        multiplexingClientManagerList.forEach(mcm -> unregisterNonExistentDevices(idsOfDeviceForWhichClientsShouldBeKept, mcm));
    }

    private void unregisterNonExistentDevices(Set<String> idsOfDeviceForWhichClientsShouldBeKept, MultiplexingClientManager mcm) {
        Set<String> mcmDeviceIDs = mcm.idsOfDevicesRegisteredAndWaitingForRegistration();
        List<String> deviceIDsToUnregister = mcmDeviceIDs.stream()
                .filter(s -> !idsOfDeviceForWhichClientsShouldBeKept.contains(s))
                .collect(Collectors.toList());

        LOG.info("Number of devices to unregister from multiplexingClientManager #{}: {}", mcm.getMultiplexingClientId(), deviceIDsToUnregister.size());
        for (String idOfNonExistentDevice : deviceIDsToUnregister) {
            try {
                LOG.info("Unregistering non-existent device {}", idOfNonExistentDevice);
                this.removeDeviceClient(idOfNonExistentDevice);
            } catch (Exception e) {
                LOG.error("Unable to unregister a non-existent device {}: {}: {}", idOfNonExistentDevice, e.getClass(), e.getMessage());
            }
        }
    }
}