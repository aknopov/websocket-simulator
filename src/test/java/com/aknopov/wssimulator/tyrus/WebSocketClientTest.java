package com.aknopov.wssimulator.tyrus;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.EventListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class WebSocketClientTest extends BaseTest {
    private final EventListener mockListener = mock(EventListener.class);

    @Test
    void testConstructor() {
        assertThrows(URISyntaxException.class, () -> new WebSocketClient("://@example.com", mockListener));
        assertDoesNotThrow(() -> new WebSocketClient("ws://@example.com:123/", mockListener));
    }

    @Test
    void testStart() throws Exception {
        WebSocketClient client = new WebSocketClient("ws://localhost:0/some/path", mockListener);
        assertFalse(client.start());
    }

    @Test
    void testGetPort() throws Exception {
        WebSocketClient client = new WebSocketClient("ws://@example.com:123/", mockListener);

        assertEquals(123, client.getPort());
    }
}