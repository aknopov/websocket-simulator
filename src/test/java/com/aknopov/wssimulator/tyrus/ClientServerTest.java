package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.aknopov.wssimulator.EventListener;
import jakarta.websocket.CloseReason;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

public class ClientServerTest extends BaseTest {
    @Test
    void TestCommunication() throws Exception {
        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(Duration.ofSeconds(1));

        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/path", server.getPort()));
        client.start();
        client.sendTextMessage(TEXT_MESSAGE);
        client.sendBinaryMessage(BINARY_MESSAGE);
        client.stop();

        Thread.sleep(200);

        InOrder inOrder = inOrder(mockListener);
        inOrder.verify(mockListener).onHandshake(any(EventListener.ProtocolHandshake.class));
        inOrder.verify(mockListener, times(2)).onOpen(anyMap());
        inOrder.verify(mockListener).onTextMessage(TEXT_MESSAGE);
        inOrder.verify(mockListener).onBinaryMessage(BINARY_MESSAGE);
        inOrder.verify(mockListener, atLeastOnce()).onClose(any(CloseReason.class));

        server.stop();
    }
}
