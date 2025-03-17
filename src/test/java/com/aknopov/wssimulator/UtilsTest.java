package com.aknopov.wssimulator;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {

    private static final String MESSAGE = "message";
    private static final int SLEEP_TIME = 100;

    @Test
    void testCheckArgument() {
        assertDoesNotThrow(() -> Utils.checkArgument(true, "message"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> Utils.checkArgument(false, "message"));
        assertEquals("message", exception.getMessage());
    }

    @Test
    void testRequireNonNull() {
        assertDoesNotThrow(() -> Utils.requireNonNull(new Object()));
        assertThrows(IllegalStateException.class, () -> Utils.requireNonNull(null));
        Throwable err = assertThrows(IllegalStateException.class, () -> Utils.requireNonNull(null, MESSAGE));
        assertEquals(MESSAGE, err.getMessage());
    }

    @Test
    void testSleepUnchecked() throws Exception {
        Thread sleepThread = new Thread(() -> Utils.sleepUnchecked(SLEEP_TIME));
        sleepThread.start();
        Thread.sleep(SLEEP_TIME / 2);
        assertDoesNotThrow(sleepThread::interrupt);

        Utils.sleepUnchecked(Duration.ofMillis(SLEEP_TIME));
    }
}