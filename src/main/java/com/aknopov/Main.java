package com.aknopov;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.tyrus.WebSocketClient;
import com.aknopov.wssimulator.tyrus.WebSocketServer;
import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.CloseReason;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(10);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        SessionConfig config = new SessionConfig("/path", IDLE_TIMEOUT, 1024000);
        EventListener listener = new EventListenerSample();
        ServiceLocator.init(config, listener);

        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of()); //+
        server.start();
        server.waitForStart(IDLE_TIMEOUT);

        logger.info("-------------------------------");
        logger.info("Server is running on port {}", server.getPort());
        logger.info("-------------------------------");

        WebSocketClient client = new WebSocketClient("ws://localhost:" + server.getPort() + "/path");
        client.start();
        client.sendTextMessage("Hello from auto!");
        client.sendBinaryMessage(ByteBuffer.wrap("Binary message".getBytes(Charset.defaultCharset())));
        client.stop();

        try (Scanner scanner = new Scanner(System.in, Charset.defaultCharset())) {
            String ignored = scanner.nextLine();
        }

        server.stop();
    }

    /**
     * An implementation od EventListener
     */
    public static class EventListenerSample implements EventListener {
        private static final Logger logger = LoggerFactory.getLogger(EventListenerSample.class);

        @Override
        public void onHandshake(ProtocolUpgrade handshake) {
            logger.info("ProtocolUpgrade: for {}, status={}", handshake.requestUri(), handshake.status());
        }

        @Override
        public void onOpen(Map<String, Object> userProperties) {
            logger.info("Connection opened. Properties have {} entries", userProperties.size());
        }

        @Override
        public void onClose(CloseReason closeReason) {
            logger.info("Connection closed. Reason - {}", closeReason);
        }

        @Override
        public void onError(Throwable error) {
            logger.error("Error happened", error);
        }

        @Override
        public void onTextMessage(String message) {
            logger.info("Text message received: {}", message);
        }

        @Override
        public void onBinaryMessage(ByteBuffer message) {
            logger.info("Binary message received: len={}", message.remaining());
        }
    }
}
