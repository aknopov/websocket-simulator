package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ToxicTimeoutTest extends ToxicTestBase {

    private Runnable mockRunner = mock(Runnable.class);

    @Test
    void testStopper() {
        ToxicTimeout toxic = new ToxicTimeout(START_DELAY, mockRunner);

        toxic.start();

        Utils.sleepUnchecked(TIME_PRECISION);
        ByteBuffer outData = toxic.transform(IN_DATA);
        verifyNoInteractions(mockRunner);
        assertEquals(IN_DATA, outData);

        Utils.sleepUnchecked(START_DELAY);
        outData = toxic.transform(IN_DATA);
        verify(mockRunner).run();
        assertEquals(IN_DATA, outData);
    }
}