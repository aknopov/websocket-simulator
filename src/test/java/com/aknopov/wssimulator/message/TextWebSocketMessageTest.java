package com.aknopov.wssimulator.message;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage.MessageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextWebSocketMessageTest {
    @Test
    void testCreation() {
        WebSocketMessage message = new TextWebSocketMessage("Text");

        assertEquals(MessageType.TEXT, message.getMessageType());
        assertEquals("Text", message.getMessageText());
        assertNull(message.getMessageBytes());
    }

    @Test
    void testEquality() {
        WebSocketMessage message1 = new TextWebSocketMessage("Text");
        WebSocketMessage message2 = new TextWebSocketMessage("Text");
        WebSocketMessage message3 = new TextWebSocketMessage("else");

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
        WebSocketMessage message = new TextWebSocketMessage("Text");

        assertEquals("TextWebSocketMessage{text='Text'}", message.toString());
    }
}