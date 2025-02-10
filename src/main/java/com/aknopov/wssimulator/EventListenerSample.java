package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.CloseReason;

/**
 * An implementation od EventListener
 */
public class EventListenerSample implements EventListener {
    private static final Logger logger = LoggerFactory.getLogger(EventListenerSample.class);

    @Override
    public void onHandshake(Handshake handshake) {
        logger.info("Handshake for {}, headers: {}", handshake.requestUri(), handshake.headers().size());
    }

    @Override
    public void onOpen(Map<String, Object> userProperties) {
        logger.info("Connection opened. Properties have {} entries", userProperties.size());
    }

    @Override
    public void onClose(CloseReason closeReason) {
        logger.info("Connection closed. Reason - {}", closeReason);
    }

    @Override
    public void onError(Throwable error) {
        logger.error("Error happened", error);
    }

    @Override
    public void onTextMessage(String message) {
        logger.info("Text message received: {}", message);
    }

    @Override
    public void onBinaryMessage(ByteBuffer message) {
        logger.info("Binary message received: len={}", message.remaining());
    }
}
