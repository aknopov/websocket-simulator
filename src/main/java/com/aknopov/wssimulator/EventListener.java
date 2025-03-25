package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.util.Map;

import jakarta.websocket.CloseReason.CloseCodes;

/**
 * WebSocket event listener
 */
public interface EventListener {

    /**
     * Callback on protocol upgrade
     *
     * @param handshake handshake info
     */
    void onHandshake(ProtocolUpgrade handshake);

    /**
     * Callback on WebSocket "open" event
     *
     * @param endpoint simulator endpoint, useful for servers
     * @param context websocket context parameters
     */
    void onOpen(SimulatorEndpoint endpoint, Map<String, Object> context);

    /**
     * Callback on WebSocket "close" event
     *
     * @param closeCode the code why the web socket has been closed
     */
    void onClose(CloseCodes closeCode);

    /**
     * Callback for "error" event
     *
     * @param error the cause of error
     */
    void onError(Throwable error);

    /**
     * Callback for receiving text message event
     *
     * @param message the message
     */
    void onTextMessage(String message);

    /**
     * Callback for receiving binary message event
     *
     * @param message the message
     */
    void onBinaryMessage(ByteBuffer message);
}
