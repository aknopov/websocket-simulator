package com.aknopov.wssimulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TimeoutExceptionTest {
    @Test
    public void testCreation() {
        TimeoutException exception = new TimeoutException("Explanation");

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals("Explanation", exception.getMessage());
    }
}