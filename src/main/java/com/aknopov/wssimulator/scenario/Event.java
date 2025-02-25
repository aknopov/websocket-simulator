package com.aknopov.wssimulator.scenario;

import java.time.Instant;

/**
 * Web Simulator event
 *
 * @param eventTime the time of the event
 * @param eventType the type of the event
 * @param isCompleted whether the event has been completed
 */
public record Event(Instant eventTime, EventType eventType, boolean isCompleted) {
}
