package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;

/**
 * ToxicInterrupter closes proxy streams after certain period of time
 */
public class ToxicInterrupter extends Toxic {
    private final Interruptible stopper;

    /**
     * Creates class instance with start delay
     *
     * @param startDelay delay from {@link Toxic#start()} to shut down connection
     * @param stopper interruptable that will shut down connection
     */
    public ToxicInterrupter(Duration startDelay, Interruptible stopper) {
        super(startDelay);
        this.stopper = stopper;
    }

    @Override
    public Iterable<ByteBuffer> transformData(ByteBuffer inData) {
        if (canStart()) {
            stopper.interrupt();
        }
        return Collections.singletonList(inData);
    }
}
