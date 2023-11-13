package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.service.registry.Device;
import com.orange.lo.sample.lo2iothub.lo.LoCommandSender;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import com.orange.lo.sample.lo2iothub.utils.Counters;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicesManager {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONNECTION_STRING_PATTERN = "HostName=%s;DeviceId=%s;SharedAccessKey=%s";

    private List<MultiplexingClientManager> multiplexingClientManagerList;
    private Map<String, IotHubClient> ioTHubClientMap;
    private LoCommandSender loCommandSender;

    private String host;
    private final int period;
    private final ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;
    private Counters counterProvider;
    private RetryPolicy<Void> messageRetryPolicy;
    private Fallback<Void> sendMessageFallback;

    public DevicesManager(String host, int period, ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint, Counters counterProvider, RetryPolicy<Void> messageRetryPolicy, Fallback<Void> sendMessageFallback) throws IotHubClientException {
        this.host = host;
        this.period = period;
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
        this.counterProvider = counterProvider;
        this.messageRetryPolicy = messageRetryPolicy;
        this.sendMessageFallback = sendMessageFallback;
        this.ioTHubClientMap = Collections.synchronizedMap(new HashMap<String, IotHubClient>());
        this.multiplexingClientManagerList = Collections.synchronizedList(new LinkedList<>());
    }
    public void setLoCommandSender(LoCommandSender loCommandSender) {
        this.loCommandSender = loCommandSender;
    }

    public synchronized boolean containsDeviceClient(String deviceClientId) {
        return ioTHubClientMap.containsKey(deviceClientId);
    }

    public synchronized void createDeviceClient(Device device) {
        DeviceClientManager deviceClientManager = new DeviceClientManager(device, host, loCommandSender, counterProvider, messageRetryPolicy, sendMessageFallback);
        MultiplexingClientManager freeMultiplexingClientManager = getFreeMultiplexingClientManager();
        deviceClientManager.setMultiplexingClientManager(freeMultiplexingClientManager);
        freeMultiplexingClientManager.registerDeviceClientManager(deviceClientManager);

        ioTHubClientMap.put(deviceClientManager.getDeviceClient().getConfig().getDeviceId(),
                new IotHubClient(deviceClientManager, freeMultiplexingClientManager));
    }

    public synchronized IotHubClient getDeviceClient(String deviceClientId) {
        return ioTHubClientMap.get(deviceClientId);
    }

    public synchronized void removeDeviceClient(String deviceClientId) throws InterruptedException, IotHubClientException, TimeoutException {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.isDeviceRegistered(deviceClientId)) {
                multiplexingClientManager.unregisterDeviceClient(ioTHubClientMap.get(deviceClientId).getDeviceClientManager());
                ioTHubClientMap.remove(deviceClientId);
            }
        }
    }

    private MultiplexingClientManager getFreeMultiplexingClientManager() {
        for (MultiplexingClientManager multiplexingClientManager : multiplexingClientManagerList) {
            if (multiplexingClientManager.hasSpace()) {
                return multiplexingClientManager;
            }
        }
        MultiplexingClientManager freeMultiplexingClientManager = new MultiplexingClientManager(host, period, connectorHealthActuatorEndpoint);
        multiplexingClientManagerList.add(freeMultiplexingClientManager);
        return freeMultiplexingClientManager;
    }
}