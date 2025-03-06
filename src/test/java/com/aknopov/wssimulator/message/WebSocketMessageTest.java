package com.aknopov.wssimulator.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketMessageTest {

    @Test
    void testMixedInequality() {
        WebSocketMessage tMessage = new TextWebSocketMessage("Message");
        WebSocketMessage bMessage =
                new BinaryWebSocketMessage(ByteBuffer.wrap("Message".getBytes(StandardCharsets.UTF_8)));

        assertNotEquals(tMessage, bMessage);
        assertNotEquals(bMessage, tMessage);
    }
}