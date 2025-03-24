package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

class ToxicSlicerTest extends ToxicTestBase {

    @ParameterizedTest
    @CsvSource({"64, 13", "65, 13", "128, 64", "19, 7"})
    void testSlicing(int bufferSize, int sliceLength) {
        ToxicSlicer toxic = new ToxicSlicer(Duration.ZERO, sliceLength);

        ByteBuffer data = ByteBuffer.allocate(bufferSize);

        int prevOffset = -1;
        for (ByteBuffer slice : toxic.transformData(data)) {
            assertThat(slice.limit(), lessThanOrEqualTo(2 * sliceLength));
            assertThat(prevOffset, lessThan(slice.arrayOffset()));
            prevOffset = slice.arrayOffset();
        }
    }

    @Test
    void testDelays() {
        long maxDelayMs = 50;
        int bufferSize = 512;
        int sliceLength = 17;
        ByteBuffer data = ByteBuffer.allocate(bufferSize);

        ToxicSlicer toxic = new ToxicSlicer(Duration.ofMillis(maxDelayMs), sliceLength);

        int sliceCount = 0;
        long sumOfDurationsMs = 0;
        Instant sliceTime = Instant.now();
        for (ByteBuffer unused : toxic.transformData(data)) {
            Instant currentTime = Instant.now();
            long durationMs = Duration.between(sliceTime, currentTime).toMillis();
            assertTrue(durationMs <= maxDelayMs + TIME_PRECISION.toMillis());
            sliceTime = currentTime;

            sliceCount++;
            sumOfDurationsMs += durationMs;
        }
        long avgDelayMs = sumOfDurationsMs / sliceCount;
        // Five sigma rule for uniform distribution - 55/38 ~ 5/sqrt(12)
        assertTrue(Math.abs(maxDelayMs - avgDelayMs) < maxDelayMs * 55 / 38);
    }
}