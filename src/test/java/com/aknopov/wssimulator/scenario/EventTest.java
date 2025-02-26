package com.aknopov.wssimulator.scenario;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {
    @Test
    void testCreation() {
        Instant anInstant = Instant.now();
        String description = " some text";

        Event event = new Event(anInstant, EventType.OPEN, false, description);

        assertEquals(anInstant, event.eventTime());
        assertEquals(EventType.OPEN, event.eventType());
        assertFalse(event.isCompleted());
        assertEquals(description, event.description());
    }
}