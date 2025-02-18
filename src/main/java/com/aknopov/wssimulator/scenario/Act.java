package com.aknopov.wssimulator.scenario;

import java.time.Duration;

/**
 * Single action in a scenario
 */
public interface Act { //UC to concrete class/record
    /**
     * Gets duration of delay before performing act
     *
     * @return duration
     */
    Duration getDelay();

    /**
     * Gets event type
     *
     * @return type
     */
    EventType getType();
}
