package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/path", server.getPort()), mockListener);
        client.start();
        client.sendTextMessage(TEXT_MESSAGE);
        client.sendBinaryMessage(BINARY_MESSAGE);
        client.stop();

        server.stop();

        ArgumentCaptor<ProtocolUpgrade> handshakeCaptor = ArgumentCaptor.forClass(ProtocolUpgrade.class);
        InOrder inOrder = inOrder(mockListener);
        inOrder.verify(mockListener).onHandshake(handshakeCaptor.capture());
        inOrder.verify(mockListener, times(2)).onOpen(any(SimulatorEndpoint.class), anyMap());
        inOrder.verify(mockListener).onTextMessage(TEXT_MESSAGE);
        inOrder.verify(mockListener).onBinaryMessage(BINARY_MESSAGE);
        inOrder.verify(mockListener, atLeastOnce()).onClose(any(CloseCode.class));

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
