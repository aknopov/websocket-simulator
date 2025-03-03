package com.aknopov.wssimulator;

import java.util.List;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;

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
