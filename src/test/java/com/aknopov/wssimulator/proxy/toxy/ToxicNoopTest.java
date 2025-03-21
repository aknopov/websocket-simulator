package com.aknopov.wssimulator.proxy.toxy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.junit.jupiter.api.Assertions.*;

class ToxicNoopTest extends ToxicTestBase {

    @Test
    void testNoEffect() {
        ToxicNoop toxic = new ToxicNoop();
        toxic.start();

        Instant testStartTime = Instant.now();
        var outData = toxic.transformData(IN_DATA);
        Instant testEndTime = Instant.now();

        assertEquals(List.of(IN_DATA), outData);
        assertTrue(Duration.between(testStartTime, testEndTime).compareTo(TIME_PRECISION) < 0);

        Utils.sleepUnchecked(100);
        testStartTime = Instant.now();
        outData = toxic.transformData(IN_DATA);
        testEndTime = Instant.now();

        assertEquals(List.of(IN_DATA), outData);
        assertTrue(Duration.between(testStartTime, testEndTime).compareTo(TIME_PRECISION) < 0);
    }
}