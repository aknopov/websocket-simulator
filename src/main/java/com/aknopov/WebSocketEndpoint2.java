package com.aknopov;

import java.io.IOException;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

// Imperative approach

public class WebSocketEndpoint2 extends Endpoint {
    ///UC @Nullable
    private MessageHandlerImpl messageHandler;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.messageHandler = new MessageHandlerImpl(session);
        session.addMessageHandler(messageHandler);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        session.removeMessageHandler(messageHandler);
        messageHandler = null;
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }

    private static class MessageHandlerImpl implements MessageHandler.Whole<String> {
        private final Session session;

        MessageHandlerImpl(Session session) {
            this.session = session;
        }

        @Override
        public void onMessage(String message) {
            System.out.println("New Text Message Received");
            if (session != null) {
                RemoteEndpoint.Basic other = session.getBasicRemote();
                try {
                    other.sendText(message);
                }
                catch (IOException e) {
                    e.printStackTrace(System.err); //UC
                }
            }
        }
    }
}
