package com.aknopov.wssimulator;

import java.time.Duration;

import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.scenario.Scenario;

/**
 * An interface to web socket simulator
 */
public interface WebSocketSimulator {
    /**
     * Gets simulator scenario
     *
     * @return the simulator scenario
     */
    Scenario getScenario();

    /**
     * Sets simulator scenario
     * @param scenario new scenario
     */
    void setScenario(Scenario scenario);

    /**
     * Gets scenario play history
     * @return the history
     */
    History getHistory();

    /**
     * Resets simulator history
     */
    void resetHistory();

    /**
     * Gets actual server port
     *
     * @return the server port
     */
    int getServerPort();

    /**
     * Starts simulator and underlying WebSocket server
     */
    void start();

    /**
     * Stops the simulator and underlying WebSocket server
     */
    void stop();

    /**
     * Restarts simulator and underlying WebSocket server on the same port
     *
     * @param coolDownPeriodMs waiting period before the new start
     */
    void restart(Duration coolDownPeriodMs);
}
