package com.aknopov;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.EventListenerSample;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.WebSocketClient;
import com.aknopov.wssimulator.WebSocketServer;
import com.aknopov.wssimulator.injection.ServiceLocator;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        SessionConfig config = new SessionConfig("/path", Duration.ofSeconds(10), 1024000);
        EventListener listener = new EventListenerSample();
        ServiceLocator.init(config, listener);

        WebSocketServer server = new WebSocketServer("localhost", "/", Map.of());
        server.start();

        logger.info("-------------------------------");
        logger.info("Server is running on port {}", server.getPort());
        logger.info("-------------------------------");

        WebSocketClient client = new WebSocketClient(String.format("ws://localhost:%d/path", server.getPort()));
        client.start();
        client.sendTextMessage("Hello from auto!");
        client.sendBinaryMessage(ByteBuffer.wrap("Binary message".getBytes(Charset.defaultCharset())));
        client.stop();

        try (Scanner scanner = new Scanner(System.in, Charset.defaultCharset())) {
            String ignored = scanner.nextLine();
        }

        server.stop();
    }
}
