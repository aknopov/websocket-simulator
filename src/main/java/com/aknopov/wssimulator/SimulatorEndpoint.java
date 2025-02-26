package com.aknopov.wssimulator;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * API for simulator's endpoint
 */
public interface SimulatorEndpoint {
    /**
     * Sends whole text message if connection is opened.
     *
     * @param message the message
     * @throws IOException if there is a problem delivering message
     */
    void sendTextMessage(String message) throws IOException;

    /**
     * Sends whole binary message if connection is opened.
     *
     * @param message the message
     * @throws IOException if there is a problem delivering message
     */
    void sendBinaryMessage(ByteBuffer message) throws IOException;

    /**
     * Closes connection.
     *
     * @throws IOException if there was an error closing the connection.
     */
    void closeConnection() throws IOException;
}

