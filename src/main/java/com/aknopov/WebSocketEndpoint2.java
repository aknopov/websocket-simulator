package com.aknopov;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

// Imperative approach

public class WebSocketEndpoint2 extends Endpoint {
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
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        session = null;
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }

    private void onTextMessage(String message) {
        System.out.println("New Text Message Received: " + message);
    }

    private void onBinaryMessage(ByteBuffer message) {
        System.out.println("New Binary Message Received: " + message.remaining());
    }

    public void sendTextMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getBasicRemote()
                    .sendText(message);
        }
    }

    public void sendBinaryMessage(ByteBuffer message) throws IOException {
        if (session != null && session.isOpen()) {
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
            return Set.of(ServerEndpointConfig.Builder.create(WebSocketEndpoint2.class, "/socket")
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
