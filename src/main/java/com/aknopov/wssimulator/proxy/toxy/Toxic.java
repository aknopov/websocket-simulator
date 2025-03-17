package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;

import com.aknopov.wssimulator.Utils;

/**
 * A Toxic is something that can be attached to a link to modify the way
 * data can be passed through (for example, by adding latency).
 * It has to be stopped explicitly - no chained toxic scenarios (yet).
 */
public abstract class Toxic {
    protected volatile boolean stopped = false;
    protected final Duration startDelay;
    @Nullable
    protected Instant startTime;
    @Nullable
    protected Thread startThread;

    /**
     * Creates class instance with start delay
     * 
     * @param startDelay delay from {@link Toxic#start()} to begin data transform
     */
    public Toxic(Duration startDelay) {
        this.startDelay = startDelay;
    }

    /**
     * Starts the toxic.
     */
    public void start() {
        startTime = Instant.now().plus(startDelay);
        startThread = Thread.currentThread();
    }

    /**
     * Stops the toxic synchronously.
     */
    @SuppressWarnings("Interruption")
    public void stop() {
        stopped = true;
        if (startThread != null && startThread != Thread.currentThread()) {
            startThread.interrupt();
        }
    }

    protected boolean canStart() {
        Utils.checkState(startTime != null, "Toxy wasn't started");
        return !stopped && !Instant.now().isBefore(startTime);
    }

    /**
     * Transforms original data by modifying it and/or applying delay.
     * 
     * @param indata original data
     * @return transformed data
     */
    public abstract ByteBuffer transform(ByteBuffer indata);
}
