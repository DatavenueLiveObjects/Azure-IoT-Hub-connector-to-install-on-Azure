package com.orange.lo.sample.lo2iothub.azure;

public enum ConnectionStatus {
    // Either the connection is closed or is in a state where the ClientManager will
    // not attempt to reconnect.
    DISCONNECTED,

    // The client manager is attempting to reconnect.
    CONNECTING,

    // The connection is established successfully.
    CONNECTED
}
