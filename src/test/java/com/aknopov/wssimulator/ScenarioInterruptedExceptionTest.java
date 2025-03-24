package com.aknopov.wssimulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioInterruptedExceptionTest {
    @Test
    public void testCreation() {
        InterruptedException cause = new InterruptedException("Thread was interrupted");
        ScenarioInterruptedException exception = new ScenarioInterruptedException(cause);

        assertEquals(cause, exception.getCause());
        assertInstanceOf(InterruptedException.class, exception.getCause());
    }
}