package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import com.aknopov.wssimulator.tyrus.WebSocketClient;
import com.aknopov.wssimulator.tyrus.WebSocketServer;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.CloseReason.CloseCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSimulatorImplTest {
    private static final long MAXIMUM_TEST_WAIT_TIME_MS = 10_000L;
    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);

    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final int PORT = 5005;
    private static final String TEXT_MESSAGE = "Hello!";
    private static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(StandardCharsets.UTF_8));
    private static final String SERVER_RESPONSE = "All is good";

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);
    private static final int UNAUTHORIZED_CODE = 401;

    private WebSocketServer mockServer = mock(WebSocketServer.class);
    private SimulatorEndpoint mockEndpoint = mock(SimulatorEndpoint.class);
    private WebSocketSimulator simulator;

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    void testPredefinedPort() {
        simulator = new WebSocketSimulatorImpl(config, PORT);
        assertEquals(PORT, simulator.getServerPort());
    }

    @Test
    void testDynamicPort() {
        simulator = new WebSocketSimulatorImpl(config, 0);
        assertNotEquals(0, simulator.getServerPort());
    }

    @Test
    void testStartFailure() {
        doThrow(IllegalStateException.class).when(mockServer).start();
        simulator = new WebSocketSimulatorImpl(config, mockServer);

        List<Event> events = simulator.getHistory().getEvents();
        assertEquals(1, events.size());
        assertEquals(EventType.ERROR, events.get(0).eventType());
    }

    @Test
    void testHistoryEvents() {
        simulator = new WebSocketSimulatorImpl(config, 0);
        simulator.stop();

        var events = simulator.getHistory()
                .getEvents();
        assertEquals(2, events.size());
        assertEquals(EventType.STARTED, events.get(0)
                .eventType());
        assertEquals(EventType.STOPPED, events.get(1)
                .eventType());

        simulator.resetHistory();
        assertEquals(0, simulator.getHistory()
                .getEvents()
                .size());
    }

    @Test
    void testScenarioGetSet() {
        simulator = new WebSocketSimulatorImpl(config, 0);
        assertNotNull(simulator.getScenario());

        Scenario mockScenario = mock(Scenario.class);
        simulator.setScenario(mockScenario);
        assertSame(mockScenario, simulator.getScenario());
    }

    @Test
    void testSendTextMessage() {
        simulator = new WebSocketSimulatorImpl(config, 0);
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new TextWebSocketMessage(TEXT_MESSAGE));

        verify(mockEndpoint).sendTextMessage(TEXT_MESSAGE);
        List<Event> events = simulator.getHistory().getEvents();
        assertEquals(2, events.size());
        assertEquals(EventType.STARTED, events.get(0).eventType());
        assertEquals(EventType.SERVER_MESSAGE, events.get(1).eventType());
        assertEquals("Text message", events.get(1).description());
    }


    @Test
    void testSendBinaryMessage() {
        simulator = new WebSocketSimulatorImpl(config, 0);
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new BinaryWebSocketMessage(BINARY_MESSAGE));

        verify(mockEndpoint).sendBinaryMessage(BINARY_MESSAGE);
        List<Event> events = simulator.getHistory().getEvents();
        assertEquals(2, events.size());
        assertEquals(EventType.STARTED, events.get(0).eventType());
        assertEquals(EventType.SERVER_MESSAGE, events.get(1).eventType());
        assertEquals("Binary message", events.get(1).description());
    }

    @Test
    void testSendMessageFailure() {
        doThrow(IllegalStateException.class).when(mockEndpoint).sendTextMessage(anyString());
        doThrow(UncheckedIOException.class).when(mockEndpoint).sendBinaryMessage(any(ByteBuffer.class));
        when(mockServer.waitForStart(any(Duration.class))).thenReturn(Boolean.TRUE);
        simulator = new WebSocketSimulatorImpl(config, mockServer);
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new TextWebSocketMessage(TEXT_MESSAGE));
        simulator.sendMessage(new BinaryWebSocketMessage(BINARY_MESSAGE));

        List<Event> errors = simulator.getHistory()
                .getEvents()
                .stream()
                .filter(e -> EventType.ERROR == e.eventType())
                .toList();

        assertEquals(2, errors.size());
        assertEquals("Attempted to send text message before establishing connection", errors.get(0).description());
        assertEquals("Can't send binary: null", errors.get(1).description());
    }

    @Test
    void testRunningSimulator() throws Exception {
        CountDownLatch allIsDone = new CountDownLatch(1); //UC

        simulator = new WebSocketSimulatorImpl(config, 0);
        simulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, Duration.ofSeconds(1))
                .sendMessage(SERVER_RESPONSE, ACTION_WAIT)
                .expectConnectionClosed(this::validateCloseCode, ACTION_WAIT)
                .perform(allIsDone::countDown, Duration.ZERO);
        simulator.start();

        WebSocketClient wsClient = new WebSocketClient("ws://localhost:" + simulator.getServerPort() + A_PATH);
        wsClient.start();
        wsClient.sendTextMessage(TEXT_MESSAGE);
        wsClient.stop();

        assertTrue(allIsDone.await(MAXIMUM_TEST_WAIT_TIME_MS, TimeUnit.MILLISECONDS)); // UC simulator.awaitCompletion(Duration waitTime)

        simulator.stop();

        assertFalse(simulator.hasErrors());
    }

    private void validateUpgrade(ProtocolUpgrade protocolUpgrade) {
        int status = protocolUpgrade.status();
        if (status != ProtocolUpgrade.SWITCH_SUCCESS_CODE) {
            throw new ValidationException("Protocol wasn't upgraded - status code = " + status);
        }
    }

    // Assuming authentication is done in handshake handler...
    private void validateUpgradeWithAuth(ProtocolUpgrade protocolUpgrade) {
        int status = protocolUpgrade.status();
        boolean hasAuthHeader = protocolUpgrade.headers().containsKey("Authorization");

        if ( !(hasAuthHeader && status == ProtocolUpgrade.SWITCH_SUCCESS_CODE
             || !hasAuthHeader && status == UNAUTHORIZED_CODE)) {
            throw new ValidationException(
                    "Improper authentication handling: status " + status + " was returned when auth header " +
                    (hasAuthHeader ? "was present" : "was not present"));
        }
    }

    private void validateTextMessage(WebSocketMessage message) throws ValidationException {
        if (message.getMessageType() != WebSocketMessage.MessageType.TEXT) {
            throw new ValidationException("Expected text message");
        }
    }

    private void validateCloseCode(CloseCode closeCode) {
        if (closeCode != CloseCodes.NORMAL_CLOSURE) {
            throw new ValidationException("Expected socket to be closed with code " + CloseCodes.NORMAL_CLOSURE.getCode());
        }
    }
}