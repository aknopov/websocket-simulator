package com.aknopov.wssimulator;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketClientTest extends BaseTest {
    @Test
    void testConstructor() {
        assertThrows(URISyntaxException.class, () -> new WebSocketClient("://@example.com"));
        assertDoesNotThrow(() -> new WebSocketClient("ws://@example.com:123/"));
    }

    @Test
    void testStart() throws Exception {
        WebSocketClient client = new WebSocketClient("ws://localhost:0/some/path");
        assertFalse(client.start());
    }
}