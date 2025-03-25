package com.aknopov.wssimulator.tyrus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.SimulatorEndpoint;
import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Implementation of web socket simulator endpoint that works as a proxy to injected EventListener
 */
public class WebSocketEndpoint extends Endpoint implements SimulatorEndpoint {
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
     * Creates the endpoint and injects event listener (server implementation)
     */
    public WebSocketEndpoint()
    {
        this.eventListener = ServiceLocator.findOrCreate(EventListener.class);
        this.sessionConfig = ServiceLocator.findOrCreate(SessionConfig.class);
    }

    /**
     * Creates the endpoint with event listener (client implementation)
     *
     * @param eventListener event listener
     */
    public WebSocketEndpoint(EventListener eventListener)
    {
        this.eventListener = eventListener;
        this.sessionConfig = ServiceLocator.findOrCreate(SessionConfig.class);
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler(new TextMessageHandler(this::onTextMessage));
        session.addMessageHandler(new BinaryMessageHandler(this::onBinaryMessage));

        session.setMaxIdleTimeout(sessionConfig.idleTimeout().toMillis()); // necessary
        session.setMaxBinaryMessageBufferSize(sessionConfig.bufferSize()); // not necessary

        // session.getUserProperties().put("started", true); // an example of storing state

        eventListener.onOpen(this, session.getUserProperties());
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("Connection closed with {}: {}", closeReason.getCloseCode(), closeReason.getReasonPhrase());
        eventListener.onClose((CloseReason.CloseCodes)closeReason.getCloseCode());
        this.session = null;
    }

    @Override
    public void onError(Session session, Throwable error) {
        logger.error("Communication error with exception '{}", error.getMessage());
        eventListener.onError(error);
    }

    private void onTextMessage(String message) {
        logger.debug("Received text message '{}'", message);
        eventListener.onTextMessage(message);
    }

    private void onBinaryMessage(ByteBuffer message) {
        logger.debug("Received binary message with {} bytes", message.remaining());
        eventListener.onBinaryMessage(message);
    }

    @Override
    public void sendTextMessage(String message) {
        if (session != null && session.isOpen()) {
            logger.debug("Sending text message '{}'", message);
            try {
                session.getBasicRemote().sendText(message);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void sendBinaryMessage(ByteBuffer message) {
        if (session != null && session.isOpen()) {
            logger.debug("Sending binary message of {} bytes", message.remaining());
            try {
                session.getBasicRemote().sendBinary(message);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void closeConnection(CloseCode closeCode) {
        try {
            if (session != null && session.isOpen()) {
                logger.debug("Closing connection with code {}", closeCode);
                session.close(new CloseReason(closeCode, ""));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        finally {
            session = null;
        }
    }

    /**
     * Tyrus requires this class to be public!
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
            return Set.of(ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, sessionConfig.contextPath())
                            .configurator(new ServerEndpointConfigurator(eventListener))
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

    private static class ServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
        private final EventListener eventListener;

        public ServerEndpointConfigurator(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            int status = (response instanceof TyrusUpgradeResponse tyrusResponse) ? tyrusResponse.getStatus() : -1;
            eventListener.onHandshake(new ProtocolUpgrade(request.getRequestURI(), request.getQueryString(),
                    request.getHeaders(), response.getHeaders(), status));
        }
    }
}
