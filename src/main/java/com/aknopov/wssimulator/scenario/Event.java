package com.aknopov.wssimulator.scenario;

import java.time.Instant;

/**
 * Web Simulator event
 *
 * @param eventTime the time of the event
 * @param eventType the type of the event
 * @param isCompleted whether the event has been completed
 */
public record Event(Instant eventTime, EventType eventType, boolean isCompleted, String description) {
    /**
     * Creates event with current timestamp
     *
     * @param eventType the type of the event
     * @param isCompleted whether the event has been completed
     * @param description event description
     * @return the event
     */
    public static Event create(EventType eventType, boolean isCompleted, String description) {
        return new Event(Instant.now(), eventType, isCompleted, description);
    }

    /**
     * Creates event with current timestamp
     *
     * @param eventType the type of the event
     * @param isCompleted whether the event has been completed
     * @return the event
     */
    public static Event create(EventType eventType, boolean isCompleted) {
        return create(eventType, isCompleted, "");
    }

    /**
     * Creates error event with current timestamp
     *
     * @param description event description
     * @return the event
     */
    public static Event error(String description) {
        return new Event(Instant.now(), EventType.ERROR, true, description);
    }
}
