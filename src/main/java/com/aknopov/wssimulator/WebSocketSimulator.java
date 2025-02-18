package com.aknopov.wssimulator;

import java.time.Duration;

import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.scenario.Scenario;

/**
 * An interface to web socket simulator
 */
public interface WebSocketSimulator {
    /**
     * Creates an empty scenario
     *
     * @return empty scenario
     */
    Scenario createScenario();

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
     *
     * @param port the port to start web socket server. If port = 0, the actual value is defined by OS. It can be obtained
     *         with calling {@link WebSocketSimulator#getServerPort()} after the server is started.
     */
    void start(int port);

    /**
     * Waits till server starts.
     *
     * @param waitDuration maximum period to wait for start
     * @return {@code true} if server started before expiry
     */
    boolean waitForStart(Duration waitDuration);

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
