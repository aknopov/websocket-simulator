package com.aknopov.wssimulator.scenario.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BinaryWebSocketMessageTest {
    @Test
    void testCreation() {
        ByteBuffer bytes = ByteBuffer.wrap("Binary".getBytes(StandardCharsets.UTF_8));
        WebSocketMessage message = new BinaryWebSocketMessage(bytes);

        assertEquals(WebSocketMessage.MessageType.BINARY, message.getMessageType());
        assertEquals(bytes, message.getMessageBytes());
        assertNull(message.getMessageText());
    }

    @Test
    void testEquality() {
        ByteBuffer bytes1 = ByteBuffer.wrap("Binary".getBytes(StandardCharsets.UTF_8));
        ByteBuffer bytes2 = ByteBuffer.wrap("else".getBytes(StandardCharsets.UTF_8));
        WebSocketMessage message1 = new BinaryWebSocketMessage(bytes1);
        WebSocketMessage message2 = new BinaryWebSocketMessage(bytes1);
        WebSocketMessage message3 = new BinaryWebSocketMessage(bytes2);

        assertEquals(message1, message1);
        assertEquals(message1, message2);
        assertEquals(message2, message1);
        assertEquals(message1.hashCode(), message2.hashCode());
        assertNotEquals(message1, message3);
        assertNotEquals(message3, message1);
        assertNotEquals(message2, message3);
    }

    @Test
    void testToString() {
        ByteBuffer bytes = ByteBuffer.wrap("Binary".getBytes(StandardCharsets.UTF_8));
        WebSocketMessage message = new BinaryWebSocketMessage(bytes);

        assertEquals("BinaryWebSocketMessage{data size=6 bytes}", message.toString());
    }
}