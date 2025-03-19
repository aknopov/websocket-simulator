package com.aknopov.wssimulator.proxy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.Scenario;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.simulator.WebSocketClientSimulator;
import com.aknopov.wssimulator.simulator.WebSocketServerSimulator;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxyIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ProxyIntegrationTest.class);

    private static final Duration ACTION_WAIT = Duration.ofMillis(500);
    private static final Duration SHORT_WAIT = Duration.ofMillis(50);
    private static final int PING_PONG_COUNT = 6;
    private static final Duration LONG_WAIT = Duration.ofSeconds(10);
    private static final String A_PATH = "/path";
    private static final String MESSAGE_1 = "Message 1";
    private static final String SERVER_RESPONSE_1 = "Coffee break";
    private static final TextWebSocketMessage PING = new TextWebSocketMessage("ping");
    private static final TextWebSocketMessage PONG = new TextWebSocketMessage("pong");

    private static final SessionConfig SERVER_CONFIG = new SessionConfig(A_PATH);
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        int serverPort = SocketFactory.getAvailablePort(); //21601
        int clientPort = SocketFactory.getAvailablePort(); //19266
        proxyConfig = new ProxyConfig(clientPort, serverPort, 60_000, 1024, ACTION_WAIT);
    }

    @Test
    void testProxyingWithoutToxy() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SERVER_CONFIG, proxyConfig.upPort());
        serverSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::onTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_1, SHORT_WAIT)
                .expectConnectionClosed(this::onClose, ACTION_WAIT);

        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .expectMessage(this::onTextMessage, ACTION_WAIT)
                .closeConnection(CloseCodes.NORMAL_CLOSURE, SHORT_WAIT);

        TcpProxy proxy = TcpProxy.createNonToxicProxy(proxyConfig, new SocketFactory());

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.awaitTermination(LONG_WAIT);

        assertFalse(serverSimulator.hasErrors());
        assertFalse(clientSimulator.hasErrors());
    }

    @Test
    void testConnectionInterruption() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SERVER_CONFIG, proxyConfig.upPort());
        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url);
        TcpProxy proxy = TcpProxy.createInterruptingProxy(proxyConfig, new SocketFactory(),
                SHORT_WAIT.multipliedBy(PING_PONG_COUNT / 2));

        Scenario serverScenario = serverSimulator.getScenario();
        Scenario clientScenario = clientSimulator.getScenario();

        serverScenario.expectConnectionOpened(ACTION_WAIT);
        clientScenario.expectConnectionOpened(ACTION_WAIT);

        for (int i = 0; i < PING_PONG_COUNT; i++) {
            clientScenario
                    .sendMessage(PING.getMessageText(), SHORT_WAIT)
                    .expectMessage(this::verifyPong, ACTION_WAIT);
            serverScenario
                    .expectMessage(this::verifyPing, ACTION_WAIT)
                    .sendMessage(PONG.getMessageText(), SHORT_WAIT);
        }
        clientScenario.closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        serverScenario.expectConnectionClosed(this::onClose, ACTION_WAIT);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.awaitTermination(LONG_WAIT);

        List<Event> serverErrors = serverSimulator.getErrors();
        List<Event> clientErrors = clientSimulator.getErrors();
        assertTrue(serverErrors.size() >= PING_PONG_COUNT - 1);
        assertTrue(clientErrors.size() >= PING_PONG_COUNT - 1);
    }

    private void onTextMessage(WebSocketMessage message) {
        logger.info("Received '{}'", message.getMessageText());
    }

    private void onClose(CloseCode closeCode) {
        logger.info("Connection closed with {}", closeCode);
    }

    private void verifyPing(WebSocketMessage message) {
        if (!PING.equals(message)) {
            throw new ValidationException("Not PING message: " + message);
        }
    }

    private void verifyPong(WebSocketMessage message) {
        if (!PONG.equals(message)) {
            throw new ValidationException("Not PONG message: " + message);
        }
    }
}
