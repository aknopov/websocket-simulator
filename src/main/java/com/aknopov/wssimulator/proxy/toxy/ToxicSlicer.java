package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Random;

import com.aknopov.wssimulator.Utils;

/**
 * ToxicSlicer slices data into multiple smaller fragments, delaying randomly their transform
 */
public class ToxicSlicer extends Toxic {
    private static final Random randomizer = new Random(Instant.now().toEpochMilli());

    private final Duration packetDelay;
    private final int averageSize;

    /**
     * Creates class instance with start delay
     *
     * @param packetDelay maximum random delay before providing sliced packets
     * @param sliceSize average size of sliced packets (max is twice larger)
     */
    public ToxicSlicer(Duration packetDelay, int sliceSize) {
        super(Duration.ZERO);
        this.packetDelay = packetDelay;
        this.averageSize = sliceSize;
    }

    @Override
    public Iterable<ByteBuffer> transformData(ByteBuffer inData) {
        return () -> new SlicesIterator(inData, packetDelay, averageSize);
    }

    private static class SlicesIterator implements Iterator<ByteBuffer> {
        private final ByteBuffer inBuffer;
        private final long packetDelayMs;
        private final int averageSize;

        private int offset = 0;

        private SlicesIterator(ByteBuffer inBuffer, Duration packetDelay, int averageSize) {
            this.inBuffer = inBuffer;
            this.packetDelayMs = packetDelay.toMillis();
            this.averageSize = averageSize;
        }

        @Override
        public boolean hasNext() {
            return offset < inBuffer.limit();
        }

        @Override
        public ByteBuffer next() {
            int sliceOffset = offset;
            int remained = inBuffer.limit() - sliceOffset;
            int sliceLength = Math.min(remained, randomizer.nextInt(1, 2 * averageSize));
            this.offset = sliceOffset + sliceLength;

            if (packetDelayMs > 0) {
                Duration sleepTime = Duration.ofMillis(randomizer.nextLong(packetDelayMs));
                Utils.sleepUnchecked(sleepTime);
            }

            return inBuffer.slice(sliceOffset, sliceLength);
        }
    }
}
