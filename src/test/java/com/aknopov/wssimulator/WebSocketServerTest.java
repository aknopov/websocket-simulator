package com.aknopov.wssimulator;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketServerTest extends BaseTest {

    private WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());

    @AfterEach
    void cleanUp() {
        server.stop();
    }

    @Test
    void testRestart() throws Exception {
        server.start();
        server.waitForStart(Duration.ofSeconds(1));
        int port1 = server.getPort();
        server.stop();

        server = new WebSocketServer("localhost", "/", Map.of());
        server.start();
        server.waitForStart(Duration.ofSeconds(1));
        int port2 = server.getPort();

        assertNotEquals(port1, port2);
    }

    @Test
    void testWaitForStart() throws Exception {
        server.start();
        assertFalse(server.waitForStart(Duration.ofNanos(10000)));
        assertTrue(server.waitForStart(Duration.ofSeconds(1)));
    }
}