package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;

class ToxicTestBase {
    static final Duration START_DELAY = Duration.ofMillis(100);
    static final Duration TIME_PRECISION = Duration.ofMillis(10);
    static final ByteBuffer IN_DATA = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
}
