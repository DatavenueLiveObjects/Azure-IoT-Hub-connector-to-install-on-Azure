package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicesManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private List<MultiplexingClientManager> multiplexingClientManagerList;
    private LoCommandSender loCommandSender;

    private String host;
    private final int period;
    private final ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;
    private final IoTDeviceProvider ioTDeviceProvider;
    private Counters counterProvider;
    private RetryPolicy<Void> messageRetryPolicy;
    private Fallback<Void> sendMessageFallback;

    public DevicesManager(String host, int period, ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint, IoTDeviceProvider ioTDeviceProvider, Counters counterProvider, RetryPolicy<Void> messageRetryPolicy, Fallback<Void> sendMessageFallback) throws IotHubClientException {
        this.host = host;
        this.period = period;
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.counterProvider = counterProvider;
        this.messageRetryPolicy = messageRetryPolicy;
        this.sendMessageFallback = sendMessageFallback;
        this.multiplexingClientManagerList = Collections.synchronizedList(new LinkedList<>());
    }

    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public synchronized boolean containsDeviceClient(String deviceClientId) {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.deviceExisted(deviceClientId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void createDeviceClient(Device device) {
        DeviceClientManager deviceClientManager = new DeviceClientManager(device, host, loCommandSender, ioTDeviceProvider, counterProvider, messageRetryPolicy, sendMessageFallback);
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

    private MultiplexingClientManager getFreeMultiplexingClientManager() {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.hasSpace()) {
                return multiplexingClientManager;
            }
        }
        MultiplexingClientManager freeMultiplexingClientManager = new MultiplexingClientManager(host, period, connectorHealthActuatorEndpoint, ioTDeviceProvider);
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