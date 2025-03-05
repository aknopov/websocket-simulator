package com.aknopov.wssimulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeoutExceptionTest {
    @Test
    public void testCreation() {
        TimeoutException exception = new TimeoutException("Explanation");

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals("Explanation", exception.getMessage());
    }

    @Test
    void testStringify() {
        TimeoutException exception = new TimeoutException("Explanation");

        assertTrue(exception.stringify().startsWith("com.aknopov.wssimulator.TimeoutException: Explanation"));
    }
}