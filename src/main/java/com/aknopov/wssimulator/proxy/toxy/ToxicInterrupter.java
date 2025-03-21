package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;

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
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public ByteBuffer transform(ByteBuffer inData) {
        if (canStart()) {
            stopper.interrupt();
        }
        return inData;
    }
}
