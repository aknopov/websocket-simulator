package com.aknopov.wssimulator.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool factory
 */
public final class NamedThreadPool {
    private static final AtomicInteger ID = new AtomicInteger();

    private NamedThreadPool() {
    }

    /**
     * Creates fixed thread pool which threads have name "prefix-id"
     * @param poolSize maximum pool size
     * @param threadNamePrefix thread name prefix
     * @return thread pool executor
     */
    public static ExecutorService createFixedPool(int poolSize, String threadNamePrefix) {
        return Executors.newFixedThreadPool(poolSize, r -> new Thread(r, threadNamePrefix + "-" + ID.incrementAndGet()));
    }
}
