package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SimulatorEndpoint;
import jakarta.websocket.CloseReason.CloseCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ClientServerTest extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientServerTest.class);

    @Test
    void TestCommunication() throws Exception {
        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(Duration.ofSeconds(1));

        logger.info("-------------------------------");
        logger.info("Server is running on port {}", server.getPort());
        logger.info("-------------------------------");

        EventListener clientListener = mock(EventListener.class);
        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/path", server.getPort()), clientListener);
        client.start();
        client.sendTextMessage(TEXT_MESSAGE);
        client.sendBinaryMessage(BINARY_MESSAGE);
        client.stop();

        server.stop();

        ArgumentCaptor<ProtocolUpgrade> handshakeCaptor = ArgumentCaptor.forClass(ProtocolUpgrade.class);
        verify(mockListener).onHandshake(handshakeCaptor.capture());
        verify(clientListener).onHandshake(any(ProtocolUpgrade.class));
        verify(mockListener).onOpen(any(SimulatorEndpoint.class), anyMap());
        verify(clientListener).onOpen(any(SimulatorEndpoint.class), anyMap());
        verify(mockListener).onTextMessage(TEXT_MESSAGE);
        verify(mockListener).onBinaryMessage(BINARY_MESSAGE);
        verify(mockListener).onClose(any(CloseCode.class));

        assertEquals(ProtocolUpgrade.SWITCH_SUCCESS_CODE, handshakeCaptor.getValue().status());
    }

    @Test
    void testWrongPath() throws Exception {
        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(Duration.ofSeconds(1));

        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/another_path", server.getPort()),
                mock(EventListener.class));
        assertFalse(client.start());
        verify(mockListener, never()).onHandshake(any());

        server.stop();
    }
}
