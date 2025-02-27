package com.aknopov.wssimulator.scenario;

/**
 * Exception thrown when there is an exception while validating WebSocketMessage event
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
