package com.aknopov.wssimulator;

import java.net.URISyntaxException;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.tyrus.WebSocketClient;

/**
 * Implementation of WebSocketSimulator for the client
 */
public class WebSocketClientSimulator extends WebSocketSimulatorBase {
    private final WebSocketClient wsClient;

    public WebSocketClientSimulator(String serverUrl) {
        super("ClientSimulator");
        try {
            this.wsClient = new WebSocketClient(serverUrl, this);
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
        wsClient.start();
        scenarioThread.start();
        history.addEvent(Event.create(EventType.STARTED));
    }

    @Override
    public void stop() {
        wsClient.stop(); //UC?
        scenario.requestStop();
        history.addEvent(Event.create(EventType.STOPPED));
    }
}
