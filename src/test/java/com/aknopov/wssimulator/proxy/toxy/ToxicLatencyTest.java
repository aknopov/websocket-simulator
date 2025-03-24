package com.aknopov.wssimulator.proxy.toxy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToxicLatencyTest extends ToxicTestBase {
    private static final Duration DATA_DELAY = Duration.ofMillis(50);
    private static final Duration JITTER_SPAN = Duration.ofMillis(30);
    private static final Duration DELAY_PLUS_JITTER = DATA_DELAY.plus(JITTER_SPAN);
    private static final Duration DELAY_MINUS_JITTER = DATA_DELAY.minus(JITTER_SPAN);

    @Test
    void testConstructor() {
        assertDoesNotThrow(() -> new ToxicLatency(Duration.ZERO, DATA_DELAY, JITTER_SPAN));
        assertDoesNotThrow(() -> new ToxicLatency(Duration.ZERO, DATA_DELAY, JITTER_SPAN.dividedBy(2)));
        assertThrows(IllegalArgumentException.class,
                () -> new ToxicLatency(Duration.ZERO, DATA_DELAY, DELAY_PLUS_JITTER));
    }

    @Test
    void testJitter() {
        ToxicLatency toxic = new ToxicLatency(START_DELAY, DATA_DELAY, JITTER_SPAN);
        toxic.start();

        Instant testStartTime = Instant.now();
        var outData = toxic.transformData(IN_DATA);
        Instant testEndTime = Instant.now();

        assertEquals(List.of(IN_DATA), outData);
        Duration duration = Duration.between(testEndTime, testStartTime);
        assertThat(duration, lessThanOrEqualTo(TIME_PRECISION));

        Utils.sleepUnchecked(START_DELAY.plus(TIME_PRECISION));

        for (int i = 0; i < 5; i++) {
            testStartTime = Instant.now();
            outData = toxic.transformData(IN_DATA);
            testEndTime = Instant.now();

            assertEquals(List.of(IN_DATA), outData);

            duration = Duration.between(testStartTime, testEndTime);
            assertThat(duration, greaterThanOrEqualTo(DELAY_MINUS_JITTER));
            assertThat(duration, lessThanOrEqualTo(DELAY_PLUS_JITTER));
        }
    }
}