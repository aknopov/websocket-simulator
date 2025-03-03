package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import com.aknopov.wssimulator.tyrus.WebSocketClient;
import com.aknopov.wssimulator.tyrus.WebSocketServer;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketServerSimulatorTest {
    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final int PORT = 5005;

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);

    private WebSocketServer mockServer = mock(WebSocketServer.class);
    private WebSocketSimulator simulator;

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    void testPredefinedPort() {
        simulator = new WebSocketServerSimulator(config, PORT);
        assertEquals(PORT, simulator.getPort());
    }

    @Test
    void testDynamicPort() {
        simulator = new WebSocketServerSimulator(config, 0);
        assertNotEquals(0, simulator.getPort());
    }

    @Test
    void testStartFailure() {
        doThrow(IllegalStateException.class).when(mockServer).start();
        simulator = new WebSocketServerSimulator(config, mockServer);

        List<Event> events = simulator.getHistory();
        assertEquals(1, events.size());
        assertEquals(EventType.ERROR, events.get(0).eventType());
    }

    @Test
    void testHistoryEvents() {
        simulator = new WebSocketServerSimulator(config, 0);
        simulator.stop();

        var events = simulator.getHistory();
        assertEquals(2, events.size());
        assertEquals(EventType.STARTED, events.get(0)
                .eventType());
        assertEquals(EventType.STOPPED, events.get(1)
                .eventType());
    }
}