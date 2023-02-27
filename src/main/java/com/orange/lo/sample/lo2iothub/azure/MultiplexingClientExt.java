package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.MultiplexingClient;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.exceptions.MultiplexingClientRegistrationException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is in charge of handling reconnection logic and registering
 * callbacks for connection status changes. It will delegate all other calls
 * other than `Open`, `Close` and registerConnectionStatusChangeCallbaack to the
 * inner client (MultiplexingClient)
 */
public class MultiplexingClientExt extends BaseClientExt {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MultiplexingClient multiplexingClient;
    private final String multiplexClientId;

    static final long DEFAULT_REGISTRATION_TIMEOUT_MILLISECONDS = 60 * 1000; // 1 minute
    static final long DEFAULT_UNREGISTRATION_TIMEOUT_MILLISECONDS = 60 * 1000; // 1 minute

    /**
     * Creates an instance of the MultiplexingClientManager
     *
     * @param multiplexingClient the multiplexing client
     * @param multiplexClientId  user defined Id for the multiplexing client.
     */
    public MultiplexingClientExt(MultiplexingClient multiplexingClient, String multiplexClientId) {
        this.multiplexingClient = multiplexingClient;
        this.multiplexClientId = multiplexClientId;
        this.multiplexingClient.setConnectionStatusChangeCallback(this, this);
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be opened.
     */
    @Override
    protected void openClient() throws IotHubClientException, IOException {
        this.multiplexingClient.open(false);
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be closed.
     */
    @Override
    protected void closeClient() {
        this.multiplexingClient.close();
    }

    /**
     * All classes that extend ClientManagerBase should implement how their inner
     * client can be identified for logging purposes.
     */
    @Override
    public String getClientId() {
        return multiplexClientId;
    }

    public MultiplexingClient getClient() {
        return multiplexingClient;
    }

    public void registerDeviceClients(Iterable<DeviceClient> deviceClients)
            throws InterruptedException, IotHubClientException, TimeoutException {
        this.registerDeviceClients(deviceClients, DEFAULT_REGISTRATION_TIMEOUT_MILLISECONDS);
    }

    public void registerDeviceClient(DeviceClient deviceClient)
            throws InterruptedException, IotHubClientException, TimeoutException {
        this.registerDeviceClient(deviceClient, DEFAULT_REGISTRATION_TIMEOUT_MILLISECONDS);
    }

    public void unregisterDeviceClients(Iterable<DeviceClient> deviceClients)
            throws InterruptedException, IotHubClientException, TimeoutException {
        this.unregisterDeviceClients(deviceClients, DEFAULT_UNREGISTRATION_TIMEOUT_MILLISECONDS);
    }

    public void unregisterDeviceClient(DeviceClient deviceClient)
            throws InterruptedException, IotHubClientException, TimeoutException {
        this.unregisterDeviceClient(deviceClient, DEFAULT_UNREGISTRATION_TIMEOUT_MILLISECONDS);
    }

    public void registerDeviceClients(Iterable<DeviceClient> deviceClients, long timeoutMilliseconds)
            throws InterruptedException, IotHubClientException, TimeoutException {
        try {
            this.multiplexingClient.registerDeviceClients(deviceClients, timeoutMilliseconds);
        } catch (MultiplexingClientRegistrationException e) {
            // When registering device clients to an active multiplexed connection, one to
            // all of the devices may fail
            // to register if they have out-of-date or otherwise incorrect connection
            // strings, for instance. The thrown exception
            // here contains a map of deviceId -> registration failure so that you can tell
            // which devices failed to register,
            // and why each device failed to register.
            LOG.error("Encountered an exception while registering devices to the active multiplexed connection: ", e);
            Map<String, Exception> registrationExceptions = e.getRegistrationExceptions();
            for (String deviceId : registrationExceptions.keySet()) {
                LOG.error("Device {} failed to register", deviceId, registrationExceptions.get(deviceId));
            }
            LOG.error("Closing client...");
            multiplexingClient.close();
            throw e;
        } catch (IotHubClientException e) {
            LOG.error("Unexpected exception thrown during device registration, closing client...", e);
            multiplexingClient.close();
            throw e;
        }
    }

    public void registerDeviceClient(DeviceClient deviceClient, long timeoutMilliseconds)
            throws InterruptedException, IotHubClientException, TimeoutException {
        try {
            this.multiplexingClient.registerDeviceClient(deviceClient, timeoutMilliseconds);
        } catch (MultiplexingClientRegistrationException e) {
            // When registering device clients to an active multiplexed connection, one to
            // all of the devices may fail
            // to register if they have out-of-date or otherwise incorrect connection
            // strings, for instance. The thrown exception
            // here contains a map of deviceId -> registration failure so that you can tell
            // which devices failed to register,
            // and why each device failed to register.
            LOG.error("Encountered an exception while registering device to the active multiplexed connection: ", e);
            Map<String, Exception> registrationExceptions = e.getRegistrationExceptions();
            for (String deviceId : registrationExceptions.keySet()) {
                LOG.error("Device {} failed to register", deviceId, registrationExceptions.get(deviceId));
            }
            LOG.error("Closing client...");
            multiplexingClient.close();
            throw e;
        } catch (IotHubClientException e) {
            LOG.error("Unexpected exception thrown during device registration, closing client...", e);
            multiplexingClient.close();
            throw e;
        }
    }

    public void unregisterDeviceClients(Iterable<DeviceClient> deviceClients, long timeoutMilliseconds)
            throws InterruptedException, IotHubClientException, TimeoutException {
        try {
            this.multiplexingClient.unregisterDeviceClients(deviceClients, timeoutMilliseconds);
        } catch (IotHubClientException e) {
            LOG.error(
                    "Encountered an exception while unregistering device from active multiplexed connection, closing client...",
                    e);
            multiplexingClient.close();
            throw e;
        }
    }

    public void unregisterDeviceClient(DeviceClient deviceClient, long timeoutMilliseconds)
            throws InterruptedException, IotHubClientException, TimeoutException {
        try {
            this.multiplexingClient.unregisterDeviceClient(deviceClient, timeoutMilliseconds);
        } catch (IotHubClientException e) {
            LOG.error(
                    "Encountered an exception while unregistering device from active multiplexed connection, closing client...",
                    e);
            multiplexingClient.close();
            throw e;
        }
    }
}
