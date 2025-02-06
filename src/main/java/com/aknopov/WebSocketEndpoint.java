package com.aknopov;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

// Declarative approach

@ServerEndpoint("/socket")
public class WebSocketEndpoint {
    @Nullable
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig ignored) {
        System.out.println("WebSocket opened: " + session.getId());
        this.session = session;

        session.getUserProperties().put("started", true); // an example of storing state
    }

    @OnClose
    public void onClose(CloseReason reason) {
        this.session = null;
        System.out.println("Closing a WebSocket due to " + reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session ignored, Throwable error) {
        error.printStackTrace(System.err); //UC
    }

    @OnMessage
    public void handleTextMessage(Session ignored, String message) {
        System.out.println("Text Message Received: " + message);
    }

    @OnMessage(maxMessageSize = 1024000)
    public void handleBinaryMessage(Session ignored, ByteBuffer buffer) {
        System.out.println("New Binary Message Received. Len=" + buffer.remaining());
    }

    public void sendMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            RemoteEndpoint.Basic other = session.getBasicRemote();
            other.sendText(message);
        }
    }

    public void sendMessage(ByteBuffer buffer) throws IOException {
        if (session != null && session.isOpen()) {
            RemoteEndpoint.Basic other = session.getBasicRemote();
            other.sendBinary(buffer);
        }
    }

    public synchronized void closeConnection() throws IOException {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}