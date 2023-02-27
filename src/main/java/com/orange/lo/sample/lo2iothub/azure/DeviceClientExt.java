package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;

/**
 * This class is in charge of handling reconnection logic and registering
 * callbacks for connection status changes. It will delegate all other calls
 * other than `Open`, `Close` and setConnectionStatusChangeCallback to the inner
 * client (DeviceClient)
 */

public class DeviceClientExt extends BaseClientExt {
    /**
     * The methods defined in the interface DeviceClientNonDelegatedFunction will be
     * called on DeviceClientManager, and not on DeviceClient.
     */
    private final DeviceClient deviceClient;

    /**
     * Creates an instance of DeviceClientManager
     * 
     * @param deviceClient                      the DeviceClient to manage
     * @param dependencyConnectionStatusTracker the dependency connection status
     *                                          tracker (it may be the
     *                                          MultiplexingClientManager object)
     */
    DeviceClientExt(DeviceClient deviceClient, ConnectionStatusTracker dependencyConnectionStatusTracker) {
        this.dependencyConnectionStatusTracker = dependencyConnectionStatusTracker;
        this.deviceClient = deviceClient;
        this.deviceClient.setConnectionStatusChangeCallback(this, this);
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be opened.
     */
    @Override
    protected void openClient() throws IotHubClientException {
        deviceClient.open(true);
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be closed.
     */
    @Override
    protected void closeClient() {
        deviceClient.close();
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be identified for logging purposes.
     */
    @Override
    public String getClientId() {
        return deviceClient.getConfig().getDeviceId();
    }

    public DeviceClient getClient() {
        return deviceClient;
    }
}
