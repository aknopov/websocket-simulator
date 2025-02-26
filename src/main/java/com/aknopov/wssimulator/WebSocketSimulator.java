package com.aknopov.wssimulator;

import java.nio.ByteBuffer;

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
     * Sends text message
     * @param message the message
     */
    void sendTextMessage(String message);

    /**
     * Sends binary message.
     * @param message the message
     */
    void sendBinaryMessage(ByteBuffer message);

    //    /**
//     * Restarts simulator and underlying WebSocket server on the same port
//     *
//     * @param coolDownPeriodMs waiting period before the new start
//     */
//TODO Needs `WebSocketServer::start(int port)`
//     void restart(Duration coolDownPeriodMs);
}
