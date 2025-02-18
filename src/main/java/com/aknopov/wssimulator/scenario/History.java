package com.aknopov.wssimulator.scenario;

import java.util.List;

/**
 * A collection of simulator events
 */
public interface History { //UC to concrete class
    List<Event> getEvents();
}
