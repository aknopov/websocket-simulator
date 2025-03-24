package com.aknopov.wssimulator;

/**
 * Wraps an {@link InterruptedException} with an unchecked exception
 * to cancel immediately running scenario.
 */
public class ScenarioInterruptedException extends RuntimeException {
    public ScenarioInterruptedException(InterruptedException ex) {
        super(ex);
    }
}
