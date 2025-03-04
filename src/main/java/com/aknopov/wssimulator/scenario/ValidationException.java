package com.aknopov.wssimulator.scenario;

/**
 * Exception thrown when expectation is no fulfilled.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
