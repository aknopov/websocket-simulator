package com.aknopov.wssimulator.proxy.toxy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ToxicInterrupterTest extends ToxicTestBase {

    private final Interruptible mockInterruptible = mock(Interruptible.class);

    @Test
    void testInterruption() {
        ToxicInterrupter toxic = new ToxicInterrupter(START_DELAY, mockInterruptible);

        toxic.start();

        Utils.sleepUnchecked(TIME_PRECISION);
        var outData = toxic.transformData(IN_DATA);
        verifyNoInteractions(mockInterruptible);
        assertEquals(List.of(IN_DATA), outData);

        Utils.sleepUnchecked(START_DELAY);
        outData = toxic.transformData(IN_DATA);
        verify(mockInterruptible).interrupt();
        assertEquals(List.of(IN_DATA), outData);
    }
}