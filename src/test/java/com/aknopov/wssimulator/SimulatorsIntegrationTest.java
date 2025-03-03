package com.aknopov.wssimulator;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

public class SimulatorsIntegrationTest {
    private static final Duration ACTION_WAIT = Duration.ofSeconds(1);
    private static final String A_PATH = "/path";
    private static final int IDLE_SECS = 1;
    private static final int BUFFER_SIZE = 1234;
    private static final String SERVER_RESPONSE = "All is good";

    private static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);
    private static final String MESSAGE_1 = "Message 1";

//    @Test
    void testRunningSimulator() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, WebSocketSimulatorBase.DYNAMIC_PORT);
        serverSimulator.getScenario()
                .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .wait(Duration.ZERO)
                .sendMessage(SERVER_RESPONSE, Duration.ZERO)
                .expectConnectionClosed(this::validateCloseCode, ACTION_WAIT)
                .perform(() -> System.out.println("** All is done **"), Duration.ZERO);
        serverSimulator.start();

        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator("ws://localhost:" + serverSimulator.getPort() + A_PATH);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, ACTION_WAIT.dividedBy(20))
                .expectMessage(this::validateTextMessage, ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        clientSimulator.start();

        serverSimulator.getScenario().awaitCompletion(ACTION_WAIT.multipliedBy(10));
        serverSimulator.stop();

        List<Event> serverEvents = serverSimulator.getHistory();
        List<Event> clientEvents = clientSimulator.getHistory();
        System.err.println(serverEvents);
        System.err.println(clientEvents);
    }

    @Test
    void testClientReconnect() {
        // server close
        // ... and reconnect with another client
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

    private void validateCloseCode(CloseCode closeCode) {
        if (closeCode != CloseCodes.NORMAL_CLOSURE) {
            throw new ValidationException("Expected socket to be closed with code " + CloseCodes.NORMAL_CLOSURE.getCode());
        }
    }
}
