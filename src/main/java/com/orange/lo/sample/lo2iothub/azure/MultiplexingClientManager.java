package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiplexingClientManager implements IotHubConnectionStatusChangeCallback {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final AtomicInteger multiplexingClientIndex = new AtomicInteger(1);

    private final MultiplexingClient multiplexingClient;
    private final int multiplexingClientId;

    public MultiplexingClientManager(String host) {
        this.multiplexingClient = new MultiplexingClient(host, IotHubClientProtocol.AMQPS, null);
        this.multiplexingClientId = multiplexingClientIndex.getAndIncrement();
        this.multiplexingClient.setConnectionStatusChangeCallback(this, this);
    }

    public MultiplexingClientManager(MultiplexingClient multiplexingClient) {
        this.multiplexingClient = multiplexingClient;
        this.multiplexingClientId = multiplexingClientIndex.getAndIncrement();
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        MultiplexingClientManager multiplexingClientManager = (MultiplexingClientManager) connectionStatusChangeContext.getCallbackContext();

        Throwable throwable = connectionStatusChangeContext.getCause();

        if (throwable == null) {
            logger.info("Connection status changed for client: {}, status: {}, reason: {}", multiplexingClientId, status, statusChangeReason);
        } else {
            logger.info("Connection status changed for client: {}, status: {}, reason: {}, error: {}", multiplexingClientId, status, statusChangeReason, throwable.getMessage());
        }

        if (status == IotHubConnectionStatus.DISCONNECTED) {
            logger.info("Reopening client: {}", multiplexingClientId);
            new Thread(() -> {
                try {
                    open();
                } catch (IotHubClientException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    public void open() throws IotHubClientException {
        logger.info("Opening client: {}", multiplexingClientId);
        this.multiplexingClient.open(true);
        logger.info("Opened client: {}", multiplexingClientId);
    }

    public void close() throws IotHubClientException {
        logger.info("Closing client: {}", multiplexingClientId);
        this.multiplexingClient.close();
        logger.info("Closed client: {}", multiplexingClientId);
    }

    public void registerDeviceClients(List<DeviceClient> deviceClients) throws IotHubClientException, InterruptedException {
        logger.info("Registering {} device clients for client: {}", deviceClients.size(), multiplexingClientId);
        this.multiplexingClient.registerDeviceClients(deviceClients);
        logger.info("Registered {} device clients for client: {}", deviceClients.size(), multiplexingClientId);
    }

    public void unregisterDeviceClient(DeviceClient deviceClient) throws IotHubClientException, InterruptedException {
        logger.info("Unregistering device client {} for client: {}", deviceClient.getConfig().getDeviceId(), multiplexingClientId);
        this.multiplexingClient.unregisterDeviceClient(deviceClient);
        logger.info("Unregistered device client {} for client: {}", deviceClient.getConfig().getDeviceId(), multiplexingClientId);
    }
}
