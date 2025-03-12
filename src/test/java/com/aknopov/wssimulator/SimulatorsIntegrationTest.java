package com.aknopov.wssimulator;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.simulator.WebSocketClientSimulator;
import com.aknopov.wssimulator.simulator.WebSocketServerSimulator;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

import static com.aknopov.wssimulator.simulator.WebSocketServerSimulator.DYNAMIC_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorsIntegrationTest {
    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);
    private static final Duration SHORT_WAIT = ACTION_WAIT.dividedBy(20);
    private static final Duration LONG_WAIT = ACTION_WAIT.multipliedBy(10);
    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final String MESSAGE_1 = "Message 1";
    private static final String MESSAGE_2 = "Message 2";
    private static final String SERVER_RESPONSE_1 = "Coffee break";
    private static final String SERVER_RESPONSE_2 = "All is good";
    private static final int UNAUTHORIZED_CODE = 401;

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);
    private static final String AUTH_HEADER = "Authorization";

    @Test
    void testRunningSimulator() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .wait(Duration.ZERO)
                .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT)
                .perform(() -> System.out.println("** All is done **"), Duration.ZERO);
        serverSimulator.start();

        String url = "ws://localhost:" + serverSimulator.getPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));
        serverSimulator.stop();

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator.hasErrors());
    }

    @Test
    void testAuthentication() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgradeWithAuth, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT)
                .perform(() -> System.out.println("** All is done **"), Duration.ZERO);
        serverSimulator.start();

        String url = "ws://localhost:" + serverSimulator.getPort() + A_PATH;
        HttpHeaders authHeaders = HttpHeaders.of(Map.of(AUTH_HEADER, List.of("token")), (t, u) -> true);
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, authHeaders);
        clientSimulator.getScenario()
                .expectProtocolUpgrade(this::validateClientUpgradeWithAuth, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));
        serverSimulator.stop();

        assertFalse(serverSimulator.hasErrors(), "Server errors: " + serverSimulator.getErrors());
        assertFalse(clientSimulator.hasErrors(), "Client errors: " + clientSimulator.getErrors());
    }

    @Test
    void testClientReconnect() throws Exception {
        CountDownLatch intermission = new CountDownLatch(1);
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, DYNAMIC_PORT);
        serverSimulator.getScenario()
                // act 1
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
                .closeConnection(CloseCodes.GOING_AWAY, Duration.ZERO)
                // intermission
                .perform(intermission::countDown, Duration.ZERO)
                // act 2
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_2, Duration.ZERO)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        serverSimulator.start();

        WebSocketClientSimulator clientSimulator1 = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator1.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateGoingAway, ACTION_WAIT);

        WebSocketClientSimulator clientSimulator2 = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator2.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_2, SHORT_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);

        clientSimulator1.start();
        intermission.await(ACTION_WAIT.toSeconds(), TimeUnit.SECONDS);
        clientSimulator2.start();

        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));

        assertFalse(serverSimulator.hasErrors(), "Server errors: " + serverSimulator.getErrors());
        assertFalse(clientSimulator1.hasErrors(), "Client 1 errors: " + clientSimulator1.getErrors());
        assertFalse(clientSimulator2.hasErrors(), "Client 2 errors: " + clientSimulator1.getErrors());
    }

    @Test
    void testScenarioInterruption() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, DYNAMIC_PORT);
        Scenario scenario = serverSimulator.getScenario();
        // Expect two connections
        for (int i = 0; i < 2; i++) {
            scenario
                    .expectConnectionOpened(ACTION_WAIT)
                    .expectMessage(this::validateTextMessage, ACTION_WAIT)
                    .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);
        }
        serverSimulator.start();

        // Play scenario of the first client only
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);

        // Interrupt
        serverSimulator.stop();
        assertTrue(serverSimulator.awaitScenarioCompletion(ACTION_WAIT));

        List<Event> errors = serverSimulator.getErrors();
        assertEquals(1, errors.size(), "Actual errors: " + errors);
        assertTrue(errors.get(0).description().startsWith("Scenario run has been interrupted:"));
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
        boolean hasAuthHeader = protocolUpgrade.reqHeaders().containsKey("Authorization");

        if ( !(hasAuthHeader && status == ProtocolUpgrade.SWITCH_SUCCESS_CODE
           || !hasAuthHeader && status == UNAUTHORIZED_CODE)) {
            throw new ValidationException(
                    "Improper authentication handling: status " + status + " was returned when auth header " +
                            (hasAuthHeader ? "was present" : "was not present"));
        }
        protocolUpgrade.respHeaders().put("WWW-Authenticate", List.of("Bearer realm=\"Protected Area\""));
    }

    // Client side validator has only headers
    private void validateClientUpgradeWithAuth(ProtocolUpgrade protocolUpgrade) {
        Map<String, List<String>> allRequestHeaders = protocolUpgrade.reqHeaders();
        Map<String, List<String>> allResponseHeaders = protocolUpgrade.respHeaders();
        if (!allRequestHeaders.containsKey(AUTH_HEADER)
         || !allRequestHeaders.containsKey("Sec-WebSocket-Key")
         || !allResponseHeaders.containsKey("Connection")
         || !allResponseHeaders.containsKey("Sec-Websocket-Accept")
         || !allResponseHeaders.containsKey("WWW-Authenticate")
        ) {
            throw new ValidationException(
                    "Improper headers in client handshake: some mandatory headers are milling");
        }
    }
}
