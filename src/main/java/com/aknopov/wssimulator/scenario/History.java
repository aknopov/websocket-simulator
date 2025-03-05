package com.aknopov.wssimulator.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of simulator events
 */
public class History {
    private final List<Event> events = new ArrayList<>();

    /**
     * Provides immutable copy of history events
     *
     * @return list of events
     */
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Adds event into the history
     *
     * @param event the event
     */
    public void addEvent(Event event) {
        events.add(event);
    }
}
