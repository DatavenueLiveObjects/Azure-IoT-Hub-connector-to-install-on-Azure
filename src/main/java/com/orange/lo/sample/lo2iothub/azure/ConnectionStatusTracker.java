package com.orange.lo.sample.lo2iothub.azure;

/**
 * Allows for getting a connection status from implementing classes.
 */
public interface ConnectionStatusTracker {
    /**
     * Gets connection status {@link ConnectionStatus} of the implementing object
     */
    ConnectionStatus getConnectionStatus();
}
