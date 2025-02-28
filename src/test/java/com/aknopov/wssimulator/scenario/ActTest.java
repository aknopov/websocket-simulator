package com.aknopov.wssimulator.scenario;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActTest {
    @Test
    void testConsumerCreation() {
        Act<String> act = new Act<>(Duration.ZERO, EventType.ACTION, String::length);

        assertEquals(Duration.ZERO, act.delay());
        assertEquals(EventType.ACTION, act.eventType());
        assertNotNull(act.consumer());
        assertNull(act.supplier());
    }

    @Test
    void testSupplierCreation() {
        Act<String> act = new Act<>(Duration.ZERO, EventType.CLIENT_MESSAGE, () -> "Hello");

        assertEquals(Duration.ZERO, act.delay());
        assertEquals(EventType.CLIENT_MESSAGE, act.eventType());
        assertNull(act.consumer());
        assertNotNull(act.supplier());
        assertEquals("Hello", act.supplier().get());
    }
}