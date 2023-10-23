package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;

public class IoTHubClient {
    private final DeviceClient deviceClient;
    private final MultiplexingClient multiplexingClient;

    public IoTHubClient(DeviceClient deviceClient, MultiplexingClient multiplexingClient) {
        this.deviceClient = deviceClient;
        this.multiplexingClient = multiplexingClient;
    }

    public void sendMessage(){
        multiplexingClient.putInCache();
        deviceClient.sendEventAsync();
    }

    public DeviceClient getDeviceClient() {
        return deviceClient;
    }

    public MultiplexingClient getMultiplexingClient() {
        return multiplexingClient;
    }
}
