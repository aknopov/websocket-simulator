package com.aknopov.wssimulator.proxy;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedThreadPoolTest {
    private static final int TEST_TIMEOUT = 200;
    private static final Callable<Integer> SLEEPY_CALLABLE = () -> {
        Thread.sleep(TEST_TIMEOUT);
        return 0;
    };

    @Test
    void testExecutorCreation() throws Exception {
        ExecutorService executor = NamedThreadPool.createFixedPool(1, "prefix");

        assertFalse(executor.isTerminated());

        Future<Integer> future = executor.submit(SLEEPY_CALLABLE);
        assertEquals(0, future.get(TEST_TIMEOUT * 3 / 2, TimeUnit.MILLISECONDS));

        executor.shutdownNow();
        assertTrue(executor.awaitTermination(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void testThreadName() throws Exception {
        AtomicReference<String> threadNameRef = new AtomicReference<>();

        ExecutorService executor = NamedThreadPool.createFixedPool(1, "prefix");
        executor.execute(() -> threadNameRef.set(Thread.currentThread().getName()));
        executor.shutdown();
        assertTrue(executor.awaitTermination(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(executor.isTerminated());

        assertTrue(threadNameRef.get().startsWith("prefix"), threadNameRef.get());
    }
}