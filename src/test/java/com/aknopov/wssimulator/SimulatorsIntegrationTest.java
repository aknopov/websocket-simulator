package com.aknopov.wssimulator;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.simulator.WebSocketClientSimulator;
import com.aknopov.wssimulator.simulator.WebSocketServerSimulator;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

import static com.aknopov.wssimulator.simulator.WebSocketServerSimulator.DYNAMIC_PORT;
import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulatorsIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorsIntegrationTest.class);

    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);
    private static final Duration SHORT_WAIT = Duration.ofMillis(50);
    private static final Duration LONG_WAIT = Duration.ofSeconds(10);
    private static final String A_PATH = "/path";
    private static final String MESSAGE_1 = "Message 1";
    private static final String MESSAGE_2 = "Message 2";
    private static final String SERVER_RESPONSE_1 = "Coffee break";
    private static final String SERVER_RESPONSE_2 = "All is good";
    private static final int UNAUTHORIZED_CODE = 401;

    private static final SessionConfig SESSION_CONFIG = new SessionConfig(A_PATH);
    private static final String AUTH_HEADER = "Authorization";

    @BeforeAll
    static void logConfig() {
        logger.debug("-------------------------");
        logger.debug("Test configuration: {}", SESSION_CONFIG);
        logger.debug("-------------------------");
    }

    @Test
    void testRunningSimulator() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SESSION_CONFIG, DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                // can be skipped - .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .wait(Duration.ZERO)
                .sendMessage(SERVER_RESPONSE_1, SHORT_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT)
                .perform(() -> System.out.println("** All is done **"), SHORT_WAIT);
        serverSimulator.start();

        String url = "ws://localhost:" + serverSimulator.getPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        assertTrue(clientSimulator.awaitScenarioCompletion(LONG_WAIT));
        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));

        assertTrue(serverSimulator.noMoreEvents());
        assertTrue(clientSimulator.noMoreEvents());

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator.hasErrors());
    }

    @Test
    void testAuthentication() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SESSION_CONFIG, DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgradeWithAuth, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);
        serverSimulator.start();

        String url = "ws://localhost:" + serverSimulator.getPort() + A_PATH;
        HttpHeaders authHeaders = HttpHeaders.of(Map.of(AUTH_HEADER, List.of("token")), (t, u) -> true);
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG, authHeaders);
        clientSimulator.getScenario()
                .expectProtocolUpgrade(this::validateClientUpgradeWithAuth, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        assertTrue(clientSimulator.awaitScenarioCompletion(LONG_WAIT));
        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator.hasErrors());
    }

    @Test
    void testClientReconnect() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SESSION_CONFIG, DYNAMIC_PORT);
        serverSimulator.getScenario()
                // act 1
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
                .closeConnection(CloseCodes.GOING_AWAY, Duration.ZERO)
                // act 2
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_2, Duration.ZERO)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        serverSimulator.start();

        WebSocketClientSimulator clientSimulator1 =
                new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH, SESSION_CONFIG);
        clientSimulator1.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, Duration.ZERO)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateGoingAway, ACTION_WAIT);

        WebSocketClientSimulator clientSimulator2 =
                new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH, SESSION_CONFIG);
        clientSimulator2.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_2, Duration.ZERO)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);

        clientSimulator1.start();
        assertTrue(clientSimulator1.awaitScenarioCompletion(LONG_WAIT));

        clientSimulator2.start();
        assertTrue(clientSimulator2.awaitScenarioCompletion(LONG_WAIT));

        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator1.hasErrors());
        assertFalse(clientSimulator2.hasErrors());
    }

    @Test
    void testScenarioInterruption() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SESSION_CONFIG, DYNAMIC_PORT);
        Scenario serverScenario = serverSimulator.getScenario();
        // Expect two connections
        for (int i = 0; i < 2; i++) {
            serverScenario
                    .expectConnectionOpened(ACTION_WAIT)
                    .expectMessage(this::validateTextMessage, ACTION_WAIT)
                    .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT);
        }
        serverSimulator.start();

        // Play scenario of the first client only
        WebSocketClientSimulator clientSimulator =
                new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH, SESSION_CONFIG);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO)
                .wait(SHORT_WAIT);
        clientSimulator.start();

        assertTrue(clientSimulator.awaitScenarioCompletion(LONG_WAIT));

        // Interrupt
        serverSimulator.stop();
        assertTrue(serverSimulator.awaitScenarioCompletion(LONG_WAIT));

        List<Event> errors = serverSimulator.getErrors();
        assertEquals(1, errors.size(), "Server errors: " + serverSimulator.getErrors());
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
        boolean hasAuthHeader = protocolUpgrade.reqHeaders().containsKey(AUTH_HEADER);

        if ( !(hasAuthHeader && status == ProtocolUpgrade.SWITCH_SUCCESS_CODE
           || !hasAuthHeader && status == UNAUTHORIZED_CODE)) {
            throw new ValidationException(
                    "Improper authentication handling: status " + status + " was returned when auth header " +
                            (hasAuthHeader ? "was present" : "was not present"));
        }
        protocolUpgrade.respHeaders().put("WWW-Authenticate", List.of("Bearer realm=\"Protected Area\""));
    }

    private static final Set<String> EXPECTED_CLIENT_HEADERS = Set.of(AUTH_HEADER, "Sec-Websocket-Key");
    private static final Set<String> EXPECTED_SERVER_HEADERS = Set.of("Connection", "Sec-Websocket-Accept", "WWW-Authenticate");

    // Client side validator has only headers - checking some of them
    private void validateClientUpgradeWithAuth(ProtocolUpgrade protocolUpgrade) {
        Map<String, List<String>> allRequestHeaders = protocolUpgrade.reqHeaders();
        Map<String, List<String>> allResponseHeaders = protocolUpgrade.respHeaders();

        Set<String> missedClientHeaders = insensitveDiff(EXPECTED_CLIENT_HEADERS, allRequestHeaders.keySet());
        Set<String> missedServerHeaders = insensitveDiff(EXPECTED_SERVER_HEADERS, allResponseHeaders.keySet());
        if (!missedClientHeaders.isEmpty() || !missedServerHeaders.isEmpty()) {
            throw new ValidationException(
                    "Missing headers in client handshake: " + missedClientHeaders + " and " + missedServerHeaders);
        }
    }

    // Case-insensitive
    Set<String> insensitveDiff(Set<String> set1, Set<String> set2) {
        Set<String> iSet1 = set1.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> iSet2 = set2.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return iSet1.stream().filter(not(iSet2::contains)).collect(Collectors.toSet());
    }
}
