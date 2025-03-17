package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * No toxic effects
 */
public class ToxicNoop extends Toxic {
    /**
     * Creates class instance with 0 start delay
     */
    public ToxicNoop() {
        super(Duration.ZERO);
    }

    @Override
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public ByteBuffer transform(ByteBuffer indata) {
        return indata;
    }
}
