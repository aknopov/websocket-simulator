package com.aknopov.wssimulator;

/**
 * Exception thrown when waiting for data is being expired.
 */
public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return String.valueOf(super.getMessage());
    }
}
