package com.orange.lo.sample.lo2iothub.exceptions;

public class IotDeviceProviderException extends RuntimeException {

    private static final long serialVersionUID = 8723908007122875989L;

    public IotDeviceProviderException(String message, Throwable t) {
        super(message, t);
    }
}
