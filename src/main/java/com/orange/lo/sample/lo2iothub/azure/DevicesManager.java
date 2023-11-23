package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

public class DevicesManager {

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
}