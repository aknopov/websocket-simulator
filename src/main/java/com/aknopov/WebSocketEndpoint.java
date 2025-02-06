package com.aknopov;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

// Programmatic approach

public class WebSocketEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);

    @Nullable
    private Session session;

    public static Class<? extends ServerApplicationConfig> getConfigClass() {
        return MyServerApplicationConfig.class;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler(new TextMessageHandler(this::onTextMessage));
        session.addMessageHandler(new BinaryMessageHandler(this::onBinaryMessage));

        session.setMaxBinaryMessageBufferSize(1024000);
        session.setMaxIdleTimeout(10000L);

        session.getUserProperties().put("started", true); // an example of storing state

        logger.debug("WebSocket opened");
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        session = null;
        logger.debug("Closing a WebSocket due to {}", closeReason.getReasonPhrase());
    }

    @Override
    public void onError(Session session, Throwable thr) {
        logger.error("Error happened", thr);
    }

    private void onTextMessage(String message) {
        logger.debug("Text Message Received: {}", message);
        try {
            sendTextMessage("Acknowledged text");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onBinaryMessage(ByteBuffer message) {
        logger.debug("Binary Message Received: len={}", message.remaining());
        try {
            sendTextMessage("Acknowledged binary");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendTextMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            logger.debug("Sending text message");
            session.getBasicRemote()
                    .sendText(message);
        }
    }

    public void sendBinaryMessage(ByteBuffer message) throws IOException {
        if (session != null && session.isOpen()) {
            logger.debug("Sending binary message");
            session.getBasicRemote()
                    .sendBinary(message);
        }
    }

    /**
     * This class should be public!
     */
    public static class MyServerApplicationConfig implements ServerApplicationConfig {

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
            return Set.of(ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, "/path")
                    .build());
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
            return Set.of();
        }

    }

    // These two can't be generic
    private record TextMessageHandler(Consumer<String> messageConsumer) implements MessageHandler.Whole<String> {
        @Override
        public void onMessage(String message) {
            messageConsumer.accept(message);
        }
    }

    private record BinaryMessageHandler(Consumer<ByteBuffer> messageConsumer) implements MessageHandler.Whole<ByteBuffer> {
        @Override
        public void onMessage(ByteBuffer message) {
            messageConsumer.accept(message);
        }
    }
}
