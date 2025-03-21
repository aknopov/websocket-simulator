package com.aknopov.wssimulator.proxy.toxy;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ToxicInterrupterTest extends ToxicTestBase {

    private Interruptible mockInterruptible = mock(Interruptible.class);

    @Test
    void testInterruption() {
        ToxicInterrupter toxic = new ToxicInterrupter(START_DELAY, mockInterruptible);

        toxic.start();

        Utils.sleepUnchecked(TIME_PRECISION);
        ByteBuffer outData = toxic.transform(IN_DATA);
        verifyNoInteractions(mockInterruptible);
        assertEquals(IN_DATA, outData);

        Utils.sleepUnchecked(START_DELAY);
        outData = toxic.transform(IN_DATA);
        verify(mockInterruptible).interrupt();
        assertEquals(IN_DATA, outData);
    }
}