package com.aknopov.wssimulator.scenario;

import java.time.Instant;

/**
 * Web Simulator event
 */
public interface Event { //UC to concrete class
    /**
     * Gets the time of the event
     *
     * @return time instant
     */
    Instant eventTime();

    /**
     * Get type of the event
     *
     * @return event type
     */
    EventType eventType(); //UC the same as action?

    /**
     * Whether the event has been completed.
     *
     * @return check result
     */
    boolean isCompleted();
}
