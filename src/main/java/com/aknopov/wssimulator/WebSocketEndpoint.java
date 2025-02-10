package com.aknopov.wssimulator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Implementation of web socket simulator endpoint tha works as a proxy to injected EventListener
 */
public class WebSocketEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);

    private final EventListener eventListener;
    private final SessionConfig sessionConfig;

    @Nullable
    private Session session;

    /**
     * Provides configuration class for creating server endpoints.
     * @return MyServerApplicationConfig class
     */
    public static Class<? extends ServerApplicationConfig> getConfigClass() {
        return MyServerApplicationConfig.class;
    }

    /**
     * Creates the endpoint and injects event listener
     */
    public WebSocketEndpoint()
    {
        eventListener = ServiceLocator.findOrCreate(EventListener.class);
        sessionConfig = ServiceLocator.findOrCreate(SessionConfig.class);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler(new TextMessageHandler(this::onTextMessage));
        session.addMessageHandler(new BinaryMessageHandler(this::onBinaryMessage));

        session.setMaxBinaryMessageBufferSize(sessionConfig.bufferSize());
        session.setMaxIdleTimeout(sessionConfig.idleTimeout().toMillis());

        // session.getUserProperties().put("started", true); // an example of storing state

        eventListener.onOpen(session.getUserProperties());
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        eventListener.onClose(closeReason);
        session = null;
    }

    @Override
    public void onError(Session session, Throwable error) {
        eventListener.onError(error);
    }

    private void onTextMessage(String message) {
        eventListener.onTextMessage(message);
    }

    private void onBinaryMessage(ByteBuffer message) {
        eventListener.onBinaryMessage(message);
    }

    /**
     * Sends whole text message if connection is opened.
     *
     * @param message the message
     * @throws IOException if there is a problem delivering message
     */
    public void sendTextMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            logger.debug("Sending text message");
            session.getBasicRemote()
                    .sendText(message);
        }
    }

    /**
     * Sends whole binary message if connection is opened.
     *
     * @param message the message
     * @throws IOException if there is a problem delivering message
     */
    public void sendBinaryMessage(ByteBuffer message) throws IOException {
        if (session != null && session.isOpen()) {
            logger.debug("Sending binary message");
            session.getBasicRemote()
                    .sendBinary(message);
        }
    }

    /**
     * Closes connection.
     *
     * @throws IOException if there was an error closing the connection.
     */
    public void closeConnection() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
        session = null;
    }

    /**
     * This class should be public!
     */
    public static class MyServerApplicationConfig implements ServerApplicationConfig {
        private final EventListener eventListener;
        private final SessionConfig sessionConfig;

        public MyServerApplicationConfig() {
            eventListener = ServiceLocator.findOrCreate(EventListener.class);
            sessionConfig = ServiceLocator.findOrCreate(SessionConfig.class);
        }

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
            return Set.of(ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, sessionConfig.path())
                            .configurator(new HandshakeInterceptor(eventListener))
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

    private static class HandshakeInterceptor extends ServerEndpointConfig.Configurator {

        private final EventListener eventListener;

        public HandshakeInterceptor(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            eventListener.onHandshake(
                    new EventListener.Handshake(request.getRequestURI(), request.getHeaders(), request.getQueryString()));
        }
    }
}
