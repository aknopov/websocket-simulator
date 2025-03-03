package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import jakarta.websocket.CloseReason.CloseCode;

/**
 * API for simulator's endpoint
 */
public interface SimulatorEndpoint {
    /**
     * Sends whole text message if connection is opened.
     *
     * @param message the message
     * @throws UncheckedIOException if there is a problem delivering message
     */
    void sendTextMessage(String message);

    /**
     * Sends whole binary message if connection is opened.
     *
     * @param message the message
     * @throws UncheckedIOException if there is a problem delivering message
     */
    void sendBinaryMessage(ByteBuffer message);

    /**
     * Closes connection.
     *
     * @param closeCode the code for the reason to close
     * @throws UncheckedIOException if there was an error closing the connection.
     */
    void closeConnection(CloseCode closeCode);
}

