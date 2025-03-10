package com.aknopov.wssimulator.proxy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.simulator.WebSocketClientSimulator;
import com.aknopov.wssimulator.simulator.WebSocketServerSimulator;
import jakarta.websocket.CloseReason;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ProxyIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ProxyIntegrationTest.class);

    private static final Duration ACTION_WAIT = Duration.ofMillis(500);
    private static final Duration SHORT_WAIT = Duration.ofMillis(50);
    private static final Duration LONG_WAIT = Duration.ofSeconds(10);
    private static final String A_PATH = "/path";
    private static final String MESSAGE_1 = "Message 1";
    private static final String SERVER_RESPONSE_1 = "Coffee break";

    private static final SessionConfig SERVER_CONFIG = new SessionConfig(A_PATH);
    private static final ProxyConfig PROXY_CONFIG = new ProxyConfig(19266, 21601, 60_000, 1024, ACTION_WAIT);

    @Test
    void testProxying() {
        WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(SERVER_CONFIG, PROXY_CONFIG.upPort());
        serverSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .expectMessage(this::onTextMessage, ACTION_WAIT)
                .sendMessage(SERVER_RESPONSE_1, SHORT_WAIT)
                .expectConnectionClosed(this::onClose, ACTION_WAIT);

        String url = "ws://localhost:" + PROXY_CONFIG.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url);
        clientSimulator.getScenario()
                .expectConnectionOpened(ACTION_WAIT)
                .sendMessage(MESSAGE_1, SHORT_WAIT)
                .expectMessage(this::onTextMessage, ACTION_WAIT)
                .closeConnection(CloseReason.CloseCodes.NORMAL_CLOSURE, SHORT_WAIT);

        TcpProxy proxy = new TcpProxy(PROXY_CONFIG, new SocketFactory());

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.awaitTermination(LONG_WAIT);

        assertFalse(serverSimulator.hasErrors(), "Server errors: " + serverSimulator.getErrors());
        assertFalse(clientSimulator.hasErrors(), "Client errors: " + clientSimulator.getErrors());
    }

    private void onTextMessage(WebSocketMessage message) {
        logger.info("Received '{}'", message.getMessageText());
    }

    private void onClose(CloseReason.CloseCode closeCode) {
        logger.info("Connection closed with {}", closeCode);
    }
}
