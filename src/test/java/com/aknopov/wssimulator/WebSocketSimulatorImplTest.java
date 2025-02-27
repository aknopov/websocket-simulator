package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebSocketSimulatorImplTest {
    private static final long MAXIMUM_TEST_WAIT_TIME_MS = 3_000L;
    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);

    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final int PORT = 5005;
    private static final String TEXT_MESSAGE = "Hello!";
    private static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(Charset.defaultCharset()));

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);
    private static final int UNAUTHORIZED_CODE = 401;

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
    void testHistoryEvents() {
        simulator = new WebSocketSimulatorImpl(0, config);
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
        simulator = new WebSocketSimulatorImpl(0, config);
        assertNotNull(simulator.getScenario());

        Scenario mockScenario = mock(Scenario.class);
        simulator.setScenario(mockScenario);
        assertSame(mockScenario, simulator.getScenario());
    }

    @Test
    @Disabled("Just to demo API")
    void testSimulatingSimulator() throws Exception {
        CountDownLatch allIsDone = new CountDownLatch(1);

        simulator = new WebSocketSimulatorImpl(0, config);
        simulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, Duration.ofSeconds(1))
                .sendMessage("All is good", ACTION_WAIT)
                .expectConnectionClosed(ACTION_WAIT)
                .perform(allIsDone::countDown, Duration.ZERO);

        simulator.start();

        assertTrue(allIsDone.await(MAXIMUM_TEST_WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        simulator.stop();

    }

    // Assuming authentication is done in handshake handler...
    private void validateUpgrade(ProtocolUpgrade protocolUpgrade) {
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
}