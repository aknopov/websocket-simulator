package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.EventListener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ClientServerTest extends BaseTest {
    private static final Duration WAIT_DURATION = Duration.ofMillis(1000);

    @Test
    void TestCommunication() throws Exception {
        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(WAIT_DURATION);

        TestEventListener clientListener = new TestEventListener();
        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/path", server.getPort()),
                clientListener, SESSION_CONFIG);
        client.start();
        client.sendTextMessage(TEXT_MESSAGE);
        client.sendBinaryMessage(BINARY_MESSAGE);
        assertTrue(serverListener.waitForBinary(WAIT_DURATION));
        client.stop();

        // Drain events
        assertTrue(serverListener.waitForClose(WAIT_DURATION));
        server.stop();

        assertTrue(serverListener.handshakeHappened());
        assertTrue(serverListener.openHappened());
        assertTrue(serverListener.textHappened());
        assertTrue(serverListener.binaryHappened());
        assertTrue(serverListener.closeHappened());
        assertTrue(clientListener.handshakeHappened());
        assertTrue(clientListener.openHappened());
        assertTrue(clientListener.closeHappened());
    }

    @Test
    void testWrongPath() throws Exception {
        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(WAIT_DURATION);

        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/another_path", server.getPort()),
                mock(EventListener.class), SESSION_CONFIG);
        assertFalse(client.start());

        assertFalse(serverListener.handshakeHappened());
        server.stop();
    }
}
