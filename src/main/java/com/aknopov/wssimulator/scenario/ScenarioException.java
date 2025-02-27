package com.aknopov.wssimulator.scenario;

/**
 * Exception thrown when scenario expectation are not fulfilled due to timeout or different order of acts.
 */
public class ScenarioException extends RuntimeException {
    public ScenarioException(String message) {
        super(message);
    }

    public ScenarioException(String message, Throwable cause) {
        super(message, cause);
    }
}
