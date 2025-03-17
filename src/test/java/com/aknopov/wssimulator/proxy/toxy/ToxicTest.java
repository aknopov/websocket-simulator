package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToxicTest  extends ToxicTestBase {
    private static final ByteBuffer OUT_DATA = ByteBuffer.allocate(0);


    private static class ToxicSample extends Toxic {
        Instant startedTime;
        Instant stoppedTime;

        ToxicSample(Duration startDelay) {
            super(startDelay);
        }

        @Override
        public void start() {
            super.start();
            startedTime = Instant.now();
        }

        @Override
        public void stop() {
            super.stop();
            stoppedTime = Instant.now();
        }

        @Override
        public ByteBuffer transform(ByteBuffer indata) {
            if (canStart()) {
                return OUT_DATA;
            }
            return indata;
        }
    }

    @Test
    void testStart() {
        ToxicSample toxic = new ToxicSample(START_DELAY);
        assertThrows(IllegalStateException.class, () -> toxic.transform(IN_DATA));

        toxic.start();
        assertEquals(IN_DATA, toxic.transform(IN_DATA));

        Utils.sleepUnchecked(START_DELAY);
        assertEquals(OUT_DATA, toxic.transform(IN_DATA));
    }

    @Test
    void testStop() {
        ToxicSample toxic = new ToxicSample(START_DELAY);
        Thread toxicThread = new Thread(toxic::start);

        Instant testStartTime = Instant.now();
        toxicThread.start();
        Utils.sleepUnchecked(START_DELAY);
        toxic.stop();
        Instant testStopTime = Instant.now();

        assertTrue(toxicThread.isInterrupted());

        assertTrue(testStartTime.isBefore(toxic.startedTime.plus(TIME_PRECISION)));
        assertTrue(testStartTime.isAfter(toxic.startedTime.minus(TIME_PRECISION)));

        assertTrue(testStopTime.isBefore(toxic.stoppedTime.plus(TIME_PRECISION)));
        assertTrue(testStopTime.isAfter(toxic.stoppedTime.minus(TIME_PRECISION)));
    }

    @Test
    void testSynchronousStop() {
        ToxicSample toxic = new ToxicSample(START_DELAY);
        toxic.start();
        toxic.stop();

        assertFalse(Thread.currentThread().isInterrupted());
    }
}