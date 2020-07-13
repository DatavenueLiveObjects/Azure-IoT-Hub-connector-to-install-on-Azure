package com.orange.lo.sample.lo2iothub.exceptions;

public class InitializationException extends RuntimeException {

    private static final long serialVersionUID = 8723908007122875989L;

    public InitializationException() {
        super();
    }

    public InitializationException(String message) {
        super(message);
    }

    public InitializationException(Throwable t) {
        super(t);
    }

    public InitializationException(String message, Throwable t) {
        super(message, t);
    }
}