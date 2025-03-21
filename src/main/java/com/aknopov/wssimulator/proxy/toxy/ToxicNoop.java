package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;

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
    public Iterable<ByteBuffer> transformData(ByteBuffer inData) {
        return Collections.singletonList(inData);
    }
}
