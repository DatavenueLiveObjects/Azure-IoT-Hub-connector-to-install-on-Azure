package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.exceptions.MultiplexingClientRegistrationException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.orange.lo.sample.lo2iothub.utils.ConnectorHealthActuatorEndpoint;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultiplexingClientManager implements IotHubConnectionStatusChangeCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final AtomicInteger multiplexingClientIndex = new AtomicInteger(1);

    private final MultiplexingClient multiplexingClient;
    private final Map<String, DeviceClientManager> multiplexedDeviceClientManagers;
    private final ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint;
    private final IoTDeviceProvider ioTDeviceProvider;
    private final int multiplexingClientId;
    private IotHubConnectionStatus multiplexedConnectionStatus;
    private List<DeviceClientManager> deviceClientManagersToRegister = new ArrayList<DeviceClientManager>();
    private Phaser phaser = new Phaser(1);
    private final Object operationLock = new Object();
    private ScheduledExecutorService registerTaskScheduler = Executors.newScheduledThreadPool(1);

    public MultiplexingClientManager(String host, int period, ConnectorHealthActuatorEndpoint connectorHealthActuatorEndpoint, IoTDeviceProvider ioTDeviceProvider) {
        this.multiplexingClient = new MultiplexingClient(host, IotHubClientProtocol.AMQPS, null);
        this.multiplexedDeviceClientManagers = new HashMap<>();
        this.connectorHealthActuatorEndpoint = connectorHealthActuatorEndpoint;
        this.ioTDeviceProvider = ioTDeviceProvider;
        this.multiplexingClientId = multiplexingClientIndex.getAndIncrement();
        this.multiplexingClient.setConnectionStatusChangeCallback(this, this);
        this.registerTaskScheduler.scheduleAtFixedRate(() -> registerDeviceClientsAsMultiplexed(), 0, period, TimeUnit.MILLISECONDS);
        this.openMultiplexingClient();
    }

    @Override
    public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
        multiplexedConnectionStatus = connectionStatusChangeContext.getNewStatus();
        IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
        Throwable throwable = connectionStatusChangeContext.getCause();

        connectorHealthActuatorEndpoint.addMultiplexingConnectionStatus(multiplexingClient, multiplexedConnectionStatus);

        if (throwable == null) {
            LOG.info("Connection status changed for multiplexing client: {}, status: {}, reason: {}", multiplexingClientId, multiplexedConnectionStatus, statusChangeReason);
        } else {
            LOG.error("Connection status changed for multiplexing client: {}, status: {}, reason: {}, error: {}", multiplexingClientId, multiplexedConnectionStatus, statusChangeReason, throwable.getMessage());
        }

        if (multiplexedConnectionStatus == IotHubConnectionStatus.DISCONNECTED) {
            new Thread(() -> {
                reconnectMultiplexingClient(multiplexingClient, multiplexingClientId);
            }).start();
        }
    }

    public void registerDeviceClientManager(DeviceClientManager deviceClientManager) {
        synchronized (this.operationLock) {
            LOG.info("Adding device client {} to be registered by multiplexing client: {}", deviceClientManager.getDeviceClient().getConfig().getDeviceId(), multiplexingClientId);
            phaser.register();
            this.deviceClientManagersToRegister.add(deviceClientManager);
        }
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    public boolean isDeviceRegistered(String deviceClientId) {
        synchronized (this.operationLock) {
            return this.multiplexedDeviceClientManagers.containsKey(deviceClientId);
        }
    }

    public boolean deviceExisted(String deviceClientId) {
        synchronized (this.operationLock) {
            return this.multiplexedDeviceClientManagers.containsKey(deviceClientId) || deviceClientManagersToRegister.stream().anyMatch(dcm -> dcm.getDeviceClient().getConfig().getDeviceId().equals(deviceClientId));
        }
    }

    public DeviceClientManager getDeviceClientManager(String deviceClientId) {
        synchronized (this.operationLock) {
            DeviceClientManager deviceClientManager = this.multiplexedDeviceClientManagers.get(deviceClientId);
            if (deviceClientManager == null) {
                deviceClientManager = deviceClientManagersToRegister.stream().filter(dcm -> dcm.getDeviceClient().getConfig().getDeviceId().equals(deviceClientId)).findFirst().get();
            }
            return deviceClientManager;
        }
    }



    public boolean hasSpace() {
        synchronized (this.operationLock) {
            return this.multiplexedDeviceClientManagers.size() + deviceClientManagersToRegister.size() < MultiplexingClient.MAX_MULTIPLEX_DEVICE_COUNT_AMQPS;
        }
    }

    public void unregisterDeviceClient(DeviceClientManager deviceClientManager) throws IotHubClientException, InterruptedException {
        unregisterDeviceClient(deviceClientManager.getDeviceClient().getConfig().getDeviceId());
    }

    public void unregisterDeviceClient(String deviceId) throws IotHubClientException, InterruptedException {
        synchronized (this.operationLock) {
            if (this.multiplexingClient.isDeviceRegistered(deviceId)) {
                this.multiplexingClient.unregisterDeviceClient(multiplexedDeviceClientManagers.get(deviceId).getDeviceClient());
                this.multiplexedDeviceClientManagers.remove(deviceId);
            } else {
                this.deviceClientManagersToRegister.removeIf(dcm -> dcm.getDeviceClient().getConfig().getDeviceId().equals(deviceId));
            }
        }
    }

    private void registerDeviceClientsAsMultiplexed()  {
        synchronized (this.operationLock) {
            if (!deviceClientManagersToRegister.isEmpty()) {
                List<DeviceClient> deviceClientList = deviceClientManagersToRegister.stream().map(dcm -> dcm.getDeviceClient()).collect(Collectors.toList());
                try {
                    this.multiplexingClient.registerDeviceClients(deviceClientList);
                    this.multiplexedDeviceClientManagers.putAll(deviceClientManagersToRegister.stream().collect(Collectors.toMap(dcm -> dcm.getDeviceClient().getConfig().getDeviceId() , dcm -> dcm)));
                    LOG.info("Registered device clients {} for multiplexing client: {}", deviceClientManagersToRegister.stream().map(dcm -> dcm.getDeviceClient().getConfig().getDeviceId()).collect(Collectors.toSet()), multiplexingClientId);
                    deviceClientManagersToRegister.clear();
                } catch (MultiplexingClientRegistrationException e) {
                    Set<String> allDeviceIds = deviceClientManagersToRegister.stream().map(dcm -> dcm.getDeviceClient().getConfig().getDeviceId()).collect(Collectors.toSet());
                    Set<String> errorDeviceIds = e.getRegistrationExceptions().keySet();
                    Set<String> registeredDeviceIds = allDeviceIds.stream().filter(id -> !errorDeviceIds.contains(id)).collect(Collectors.toSet());

                    LOG.info("Registered device clients {} for multiplexing client: {}", registeredDeviceIds, multiplexingClientId);
                    this.multiplexedDeviceClientManagers.putAll(registeredDeviceIds.stream().collect(Collectors.toMap(Function.identity(), id -> deviceClientManagersToRegister.stream().filter(dcm -> dcm.getDeviceClient().getConfig().getDeviceId().equals(id)).findFirst().get())));
                    deviceClientManagersToRegister.removeIf(dcm -> registeredDeviceIds.contains(dcm.getDeviceClient().getConfig().getDeviceId()));

                    LOG.error("Error while registering device clients {} for multiplexing client: {}", errorDeviceIds, multiplexingClientId, e);

                    List<String> deviceIdsToNotRetry = new ArrayList<>();
                    errorDeviceIds.forEach(id -> {
                        try {
                            if (!ioTDeviceProvider.deviceExists(id)) {
                                deviceIdsToNotRetry.add(id);
                            }
                        } catch (Exception ex) {
                            LOG.error("Error while checking if device {} exists", id, ex);
                        }
                    });
                    LOG.error("Devices {} will not be retried to register because do not exist", deviceIdsToNotRetry);
                    deviceClientManagersToRegister.removeIf(dcm -> deviceIdsToNotRetry.contains(dcm.getDeviceClient().getConfig().getDeviceId()));

                    LOG.info("Devices {} will be retried to register", deviceClientManagersToRegister);
                } catch (InterruptedException | IotHubClientException e) {
                    LOG.error("Error while registering device clients {} for multiplexing client: {}", deviceClientManagersToRegister, multiplexingClientId, e);
                } finally {
                    phaser.arrive();
                }
            }
        }
    }

    private void reconnectMultiplexingClient(MultiplexingClient multiplexingClient, int clientNo) {
        Failsafe.with(getReconnectRetryPolicy()).run(() -> {
            LOG.info("Reconnecting MultiplexingClient nr {} ", clientNo);
            LOG.info("Closing MultiplexingClient nr {} ", clientNo);
            multiplexingClient.close();
            LOG.info("Closing MultiplexingClient nr {} success", clientNo);
            LOG.info("Opening MultiplexingClient nr {}", clientNo);
            multiplexingClient.open(true);
            LOG.info("Opening MultiplexingClient nr {} success", clientNo);
            LOG.info("Reconnecting MultiplexingClient nr {} success", clientNo);
        });
    }

    private void openMultiplexingClient() {
        Failsafe.with(getOpenRetryPolicy()).run(() -> {
            LOG.info("Opening MultiplexingClient nr {}", multiplexingClientId);
            multiplexingClient.open(true);
            LOG.info("Opening MultiplexingClient nr {} success", multiplexingClientId);
        });
    }

    private RetryPolicy<Void> getOpenRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> {
                    LOG.info("Opening MultiplexingClient nr {} error because of {}, retrying ...", multiplexingClientId, e.getLastFailure().getMessage());
                });
    }

    private RetryPolicy<Void> getReconnectRetryPolicy() {
        return new RetryPolicy<Void>()
                .withMaxAttempts(-1)
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .onRetry(e -> LOG.error("Reconnecting MultiplexingClient nr " + multiplexingClientId + " error because of " + e.getLastFailure().getMessage() + ", retrying ...", e.getLastFailure()));
    }

    public IotHubConnectionStatus getMultiplexedConnectionStatus() {
        return multiplexedConnectionStatus;
    }
}