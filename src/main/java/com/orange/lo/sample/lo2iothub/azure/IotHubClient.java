package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;

public class IotHubClient {
    private final DeviceClientManager deviceClientManager;
    private final MultiplexingClientManager multiplexingClientManager;


    public IotHubClient(DeviceClientManager deviceClientManager, MultiplexingClientManager freeMultiplexingClientManager) {
        this.deviceClientManager = deviceClientManager;
        this.multiplexingClientManager = freeMultiplexingClientManager;
    }

    public DeviceClientManager getDeviceClientManager() {
        return deviceClientManager;
    }

    public MultiplexingClientManager getMultiplexingClientManager() {
        return multiplexingClientManager;
    }
}
