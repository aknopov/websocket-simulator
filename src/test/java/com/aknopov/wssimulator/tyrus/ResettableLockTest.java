package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.ResettableLock;
import com.aknopov.wssimulator.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResettableLockTest {

    private static final long WAIT_TIME_MSEC = 200;
    private static final long MAX_WAIT_MSEC = 220;
    private static final int USES_COUNT = 3;

    @Test
    void testReusability() throws Exception {
        ResettableLock<Boolean> rl = new ResettableLock<>();

        for (int i = 0; i < USES_COUNT; i++) {
            new Thread(() -> {
                try {
                    Thread.sleep(WAIT_TIME_MSEC);
                    rl.release(Boolean.TRUE);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            Instant start = Instant.now();
            assertTrue(rl.await(Duration.ofSeconds(1)));
            Duration timeout = Duration.between(start, Instant.now());
            assertTrue(timeout.toMillis() < MAX_WAIT_MSEC);
        }
    }

    @Test
    void testSimultaneousRelease() throws Exception {

        ResettableLock<Boolean> rl = new ResettableLock<>();

        for (int i = 0; i < USES_COUNT; i++) {
            new Thread(() -> {
                try {
                    Thread.sleep(WAIT_TIME_MSEC);
                    rl.release(Boolean.TRUE);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        Instant start = Instant.now();
        assertEquals(Boolean.TRUE, rl.await(Duration.ofSeconds(1)));
        Duration timeout = Duration.between(start, Instant.now());
        assertTrue(timeout.toMillis() < MAX_WAIT_MSEC);
    }

    @Test
    void testWaitInterval() {
        ResettableLock<Boolean> rl = new ResettableLock<>();
        Instant start = Instant.now();

        assertThrows(TimeoutException.class, () -> rl.await(Duration.ofMillis(WAIT_TIME_MSEC)));

        Duration timeout = Duration.between(start, Instant.now());
        assertTrue(timeout.toMillis() < MAX_WAIT_MSEC);
    }
}
