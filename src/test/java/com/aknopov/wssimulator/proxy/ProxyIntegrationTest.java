package com.aknopov.wssimulator.proxy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.Scenario;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.WebSocketSimulator;
import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.simulator.WebSocketClientSimulator;
import com.aknopov.wssimulator.simulator.WebSocketServerSimulator;
import jakarta.websocket.CloseReason.CloseCodes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ProxyIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ProxyIntegrationTest.class);

    private static final Duration ACTION_WAIT = Duration.ofMillis(1_000);
    private static final Duration SHORT_WAIT = Duration.ofMillis(50);
    private static final Duration LONG_WAIT = Duration.ofMillis(10_000);
    private static final int PING_PONG_COUNT = 6;
    private static final String A_PATH = "/path";
    private static final TextWebSocketMessage PING = new TextWebSocketMessage("ping");
    private static final TextWebSocketMessage PONG = new TextWebSocketMessage("pong");

    private static final SessionConfig SESSION_CONFIG = new SessionConfig(A_PATH);

    private ProxyConfig proxyConfig;
    private WebSocketServerSimulator serverSimulator;

    @BeforeEach
    void setUp() {
        configureTestWithServerTimeout(SESSION_CONFIG.idleTimeout());
    }

    private void configureTestWithServerTimeout(Duration idleTimeout) {
        int serverPort = SocketFactory.getAvailablePort(); //21601
        int clientPort = SocketFactory.getAvailablePort(); //19266
        proxyConfig = new ProxyConfig(clientPort, serverPort, 60_000, 1024, ACTION_WAIT.multipliedBy(PING_PONG_COUNT));
        SessionConfig serverConfig = new SessionConfig(A_PATH, idleTimeout, 1024);
        serverSimulator = new WebSocketServerSimulator(serverConfig, proxyConfig.upPort());
    }

    @AfterEach
    void tearDown() {
        serverSimulator.stop();
    }

    @Test
    void testProxyingWithoutToxy() {
        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        TcpProxy proxy = TcpProxy.createNonToxicProxy(proxyConfig);

        configureScenarios(serverSimulator, clientSimulator, 1);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.stop();

        assertNoErrors(serverSimulator, "Server errors");
        assertNoErrors(clientSimulator, "Client errors");
    }

    @Test
    void testConnectionInterruption() {
        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        TcpProxy proxy = TcpProxy.createInterruptingProxy(proxyConfig, SHORT_WAIT.multipliedBy(PING_PONG_COUNT / 2),
                new SocketFactory());

        configureScenarios(serverSimulator, clientSimulator, PING_PONG_COUNT);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.stop();

        List<Event> serverErrors = serverSimulator.getErrors();
        List<Event> clientErrors = clientSimulator.getErrors();
        assertFalse(serverErrors.isEmpty());
        assertFalse(clientErrors.isEmpty());
        assertTrue(serverErrors.stream().allMatch(e -> e.description().contains("TimeoutException")));
        assertTrue(clientErrors.stream().allMatch(e -> e.description().contains("TimeoutException")));
    }

    // This test is susceptible to NIC/CPU speed
    @Test
    void testLowJitterLevel() {
        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        TcpProxy proxy = TcpProxy.createJitterProxy(proxyConfig, Duration.ZERO, Duration.ofMillis(50),
                Duration.ofMillis(10), new SocketFactory());

        configureScenarios(serverSimulator, clientSimulator, PING_PONG_COUNT);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.stop();

        assertNoErrors(serverSimulator, "Server");
        assertNoErrors(clientSimulator, "Client");
    }

    // This test is susceptible to NIC/CPU speed
    @Test
    void testHighJitterLevel() {
        // Using low idle timeout requires new server
        configureTestWithServerTimeout(Duration.ofMillis(400));

        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        TcpProxy proxy = TcpProxy.createJitterProxy(proxyConfig, Duration.ZERO, Duration.ofMillis(200),
                Duration.ofMillis(200), new SocketFactory());

        configureScenarios(serverSimulator, clientSimulator, PING_PONG_COUNT);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.stop();

        assertTrue(serverSimulator.hasErrors());
        assertTrue(clientSimulator.hasErrors());
    }

    @Test
    void testNonToxicSlicer() {
        // Using low idle timeout requires new server
        configureTestWithServerTimeout(Duration.ofSeconds(1));

        String url = "ws://localhost:" + proxyConfig.downPort() + A_PATH;
        WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator(url, SESSION_CONFIG);
        TcpProxy proxy = TcpProxy.createSlicerProxy(proxyConfig, 32, Duration.ofMillis(50), new SocketFactory());

        configureScenarios(serverSimulator, clientSimulator, PING_PONG_COUNT);

        proxy.start();
        serverSimulator.start();
        clientSimulator.start();

        clientSimulator.awaitScenarioCompletion(LONG_WAIT);
        serverSimulator.awaitScenarioCompletion(LONG_WAIT);
        proxy.stop();

        assertNoErrors(serverSimulator, "Server");
        assertNoErrors(clientSimulator, "Client");
    }

    private void configureScenarios(WebSocketServerSimulator serverSimulator, WebSocketClientSimulator clientSimulator,
            int messageCount) {
        Scenario serverScenario = serverSimulator.getScenario();
        Scenario clientScenario = clientSimulator.getScenario();

        serverScenario.expectConnectionOpened(ACTION_WAIT);
        clientScenario.expectConnectionOpened(ACTION_WAIT);

        for (int i = 0; i < messageCount; i++) {
            clientScenario
                    .sendMessage(PING.getMessageText(), SHORT_WAIT)
                    .expectMessage(this::verifyPong, ACTION_WAIT);
            serverScenario
                    .expectMessage(this::verifyPing, ACTION_WAIT)
                    .sendMessage(PONG.getMessageText(), SHORT_WAIT);
        }
        clientScenario.closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
        serverScenario.expectConnectionClosed(this::onClose, ACTION_WAIT);
    }

    private void onClose(CloseCodes closeCode) {
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

    private void assertNoErrors(WebSocketSimulator simulator, String hint) {
        List<Event> errors = simulator.getErrors();
        if (!errors.isEmpty()) {
            logger.error("{} errors:", hint);
            errors.forEach(e -> logger.error("    {}", e));
            fail();
        }
    }
}
