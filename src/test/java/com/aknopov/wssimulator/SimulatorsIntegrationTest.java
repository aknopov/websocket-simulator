package com.aknopov.wssimulator;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorsIntegrationTest {
    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);
    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final String MESSAGE_1 = "Message 1";
    private static final String MESSAGE_2 = "Message 2";
    private static final String SERVER_RESPONSE_1 = "Coffee break";
    private static final String SERVER_RESPONSE_2 = "All is good";
    private static final int UNAUTHORIZED_CODE = 401;

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);

    @Test
    void testRunningSimulator() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, WebSocketSimulatorBase.DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .wait(Duration.ZERO)
                .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT)
                .perform(() -> System.out.println("** All is done **"), Duration.ZERO);
        serverSimulator.start();

        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, ACTION_WAIT.dividedBy(20))
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        assertTrue(serverSimulator.getScenario().awaitCompletion(ACTION_WAIT.multipliedBy(10)));
        serverSimulator.stop();

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator.hasErrors());
    }

    @Test
    void testClientReconnect() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, WebSocketSimulatorBase.DYNAMIC_PORT);
        serverSimulator.getScenario()
                // client 1
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
                .closeConnection(CloseCodes.GOING_AWAY, Duration.ZERO)
                // client 2
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_2, Duration.ZERO)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        serverSimulator.start();

        WebSocketClientSimulator clientSimulator1 = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator1.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, ACTION_WAIT.dividedBy(20))
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateGoingAway, ACTION_WAIT);
        clientSimulator1.start();

        WebSocketClientSimulator clientSimulator2 = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator2.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_2, ACTION_WAIT.dividedBy(20))
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);
        clientSimulator2.start();

        assertTrue(serverSimulator.getScenario().awaitCompletion(ACTION_WAIT.multipliedBy(10)));
        serverSimulator.stop();
    }

    private void validateUpgrade(ProtocolUpgrade protocolUpgrade) {
        int status = protocolUpgrade.status();
        if (status != ProtocolUpgrade.SWITCH_SUCCESS_CODE) {
            throw new ValidationException("Protocol wasn't upgraded - status code = " + status);
        }
    }

    private void validateTextMessage(WebSocketMessage message) throws ValidationException {
        if (message.getMessageType() != WebSocketMessage.MessageType.TEXT) {
            throw new ValidationException("Expected text message");
        }
    }

    private void validateNormalClose(CloseCode closeCode) {
        validateCloseCode(closeCode, CloseCodes.NORMAL_CLOSURE);
    }

    private void validateGoingAway(CloseCode closeCode) {
        validateCloseCode(closeCode, CloseCodes.GOING_AWAY);
    }

    private void validateCloseCode(CloseCode closeCode, CloseCode expectedCode) {
        if (closeCode != expectedCode) {
            throw new ValidationException("Expected socket to be closed with code " + expectedCode.getCode());
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
}
