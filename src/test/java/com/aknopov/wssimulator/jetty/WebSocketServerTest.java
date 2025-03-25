package com.aknopov.wssimulator.jetty;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketServerTest extends BaseTest {

    private WebSocketServer server = new WebSocketServer("localhost", "/", SESSION_CONFIG);

    @AfterEach
    void cleanUp() {
        server.stop();
    }

    @Test
    void testRestart() {
        server.start();
        server.waitForStart(Duration.ofSeconds(1));
        int port1 = server.getPort();
        server.stop();

        server = new WebSocketServer("localhost", "/", SESSION_CONFIG);
        server.start();
        server.waitForStart(Duration.ofSeconds(1));
        int port2 = server.getPort();

        assertNotEquals(port1, port2);
    }

    @Test
    void testWaitForStart() {
        server.start();
        assertFalse(server.waitForStart(Duration.ofNanos(10000)));
        assertTrue(server.waitForStart(Duration.ofSeconds(1)));
    }

    @Test
    void testForStop() {
        server.start();
        assertFalse(server.waitForStop(Duration.ofNanos(10000)));
        server.stop();
        assertFalse(server.waitForStop(Duration.ofNanos(10000)));
        assertTrue(server.waitForStop(Duration.ofSeconds(1)));
    }

    @Test
    void testErrorOnDoubleStart() {
        server.start();
        server.waitForStart(Duration.ofSeconds(1));

        assertThrows(IllegalStateException.class, () -> server.start());
    }
}