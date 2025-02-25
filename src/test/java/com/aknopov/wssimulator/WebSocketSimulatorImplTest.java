package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WebSocketSimulatorImplTest {

    protected static final String A_PATH = "/path";
    protected static final int IDLE_SECS = 1;
    protected static final int BUFFER_SIZE = 1234;
    private static final int PORT = 5005;
    protected static final String TEXT_MESSAGE = "Hello!";
    protected static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(Charset.defaultCharset()));

    protected static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);

    private WebSocketSimulatorImpl simulator;

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    void testPredefinedPort() {
        simulator = new WebSocketSimulatorImpl(PORT, config);
        assertEquals(PORT, simulator.getServerPort());
    }

    @Test
    void testDynamicPort() {
        simulator = new WebSocketSimulatorImpl(0, config);
        assertNotEquals(0, simulator.getServerPort());
    }

    @Test
    void testWhatAboutHistory() {
        simulator = new WebSocketSimulatorImpl(0, config);
        simulator.start();
        simulator.stop();

        assertFalse(simulator.getHistory().getEvents().isEmpty());
        System.err.println(simulator.getHistory().getEvents());//UC
    }
}