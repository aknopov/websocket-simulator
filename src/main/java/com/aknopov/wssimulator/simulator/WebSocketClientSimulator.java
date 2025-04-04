package com.aknopov.wssimulator.simulator;

import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.time.Duration;

import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.tyrus.WebSocketClient;

/**
 * Implementation of WebSocketSimulator for the client
 */
public class WebSocketClientSimulator extends WebSocketSimulatorBase {
    private static final Duration OPEN_WAIT_DURATION = Duration.ofSeconds(1);
    private final WebSocketClient wsClient;

    public WebSocketClientSimulator(String serverUrl, SessionConfig sessionConfig) {
        super("Client");
        try {
            this.wsClient = new WebSocketClient(serverUrl, this, sessionConfig);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't connect to '" + serverUrl + "'", e);
        }
    }


    public WebSocketClientSimulator(String serverUrl, SessionConfig sessionConfig, HttpHeaders extraHeaders) {
        super("Client");
        try {
            this.wsClient = new WebSocketClient(serverUrl, this, sessionConfig, extraHeaders);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't connect to '" + serverUrl + "'", e);
        }
    }

    @Override
    public int getPort() {
        return wsClient.getPort();
    }

    @Override
    public void start() {
        // Start scenario first to wait on open
        super.start();
        awaitScenarioStart(OPEN_WAIT_DURATION);
        // ... then client
        wsClient.start();
        history.addEvent(Event.create(EventType.STARTED));
    }

    @Override
    public void stop() {
        wsClient.stop();
        super.stop();
    }
}
