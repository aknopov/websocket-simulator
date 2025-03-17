package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * ToxicTimeout closes proxy streams after certain period of time
 */
public class ToxicTimeout extends Toxic {
    private final Runnable stopper;

    /**
     * Creates class instance with start delay
     *
     * @param startDelay delay from {@link Toxic#start()} to shut down connection
     * @param stopper runnable that will shut down connection
     */
    public ToxicTimeout(Duration startDelay, Runnable stopper) {
        super(startDelay);
        this.stopper = stopper;
    }

    @Override
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public ByteBuffer transform(ByteBuffer indata) {
        if (canStart()) {
            stopper.run();
        }
        return indata;
    }
}
