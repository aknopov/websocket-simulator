package com.aknopov.wssimulator.scenario;

import java.time.Instant;

/**
 * Web Simulator event
 *
 * @param eventTime the time of the event
 * @param eventType the type of the event
 * @param description optional description
 */
public record Event(Instant eventTime, EventType eventType, String description) {
    /**
     * Creates event with current timestamp
     *
     * @param eventType the type of the event
     * @param description event description
     *
     * @return the event
     */
    public static Event create(EventType eventType, String description) {
        return new Event(Instant.now(), eventType, description);
    }

    /**
     * Creates event with current timestamp
     *
     * @param eventType the type of the event
     *
     * @return the event
     */
    public static Event create(EventType eventType) {
        return create(eventType, "");
    }

    /**
     * Creates error event with current timestamp
     *
     * @param description event description
     * @return the event
     */
    public static Event error(String description) {
        return new Event(Instant.now(), EventType.ERROR, description);
    }
}
