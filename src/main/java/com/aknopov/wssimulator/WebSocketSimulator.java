package com.aknopov.wssimulator;

import java.time.Duration;
import java.util.List;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.message.WebSocketMessage;

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
     * Sets the only simulator endpoint to be able to act on server side
     *
     * @param endpoint the endpoint
     */
    void setEndpoint(SimulatorEndpoint endpoint);

    /**
     * Gets scenario play history
     *
     * @return list of events
     */
    List<Event> getHistory();

    /**
     * Gets actual port of the communication
     *
     * @return the server port
     */
    int getPort();

    /**
     * Starts simulator and underlying WebSocket server
     */
    void start();

    /**
     * Stops the simulator and underlying WebSocket server
     */
    void stop();

    /**
     * Sends text message
     *
     * @param message the message
     */
    void sendMessage(WebSocketMessage message);

    /**
     * Waits for scenario to be completed
     *
     * @param waitDuration wait duration
     * @return {@code true} if scenario completed before wait expiry
     * @throws ScenarioInterruptedException if wait was interrupted
     */
    boolean awaitScenarioStart(Duration waitDuration);

    /**
     * Waits for scenario to be completed
     *
     * @param waitDuration wait duration
     * @return {@code true} if scenario completed before wait expiry
     * @throws ScenarioInterruptedException if wait was interrupted
     */
    boolean awaitScenarioCompletion(Duration waitDuration);

    /**
     * Checks if all scenario acts were performed
     *
     * @return check result
     */
    boolean isScenarioDone();

    /**
     * Checks if errors encountered while playing scenario
     *
     * @return check result
     */
    boolean hasErrors();

    /**
     * Gets list of recorded scenario errors
     *
     * @return the list
     */
    List<Event> getErrors();
}
