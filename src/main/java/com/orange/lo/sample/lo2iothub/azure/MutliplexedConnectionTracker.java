package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

public interface MutliplexedConnectionTracker {
    IotHubConnectionStatus getMultiplexedConnectionStatus();
}
