package com.aknopov.wssimulator;

import java.util.Map;

import com.aknopov.wssimulator.injection.ServiceLocator;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.tyrus.WebSocketServer;

/**
 * Implementation of WebSocketSimulator for the server
 */
public class WebSocketServerSimulator extends WebSocketSimulatorBase {
    public static final int DYNAMIC_PORT = 0;
    private final WebSocketServer wsServer;

    /**
     * Creates simulator with given configuration
     *
     * @param config session configuration
     * @param port port number, 0 - is for dynamic
     */
    public WebSocketServerSimulator(SessionConfig config, int port) {
        this(config, port != 0
                ? new WebSocketServer("localhost", "/", Map.of(), port)
                : new WebSocketServer("localhost", "/", Map.of()));
    }

    //VisibleForTesting
    WebSocketServerSimulator(SessionConfig config, WebSocketServer wsServer) {
        super("ServerSimulator");
        this.wsServer = wsServer;
        ServiceLocator.init(config, this);
        startServer(config);
    }

    @SuppressWarnings("NullAway")
    private void startServer(SessionConfig config) {
        try {
            wsServer.start();
            if (!wsServer.waitForStart(config.idleTimeout())) {
                throw new TimeoutException("Wait for server start timed out");
            }
            history.addEvent(Event.create(EventType.STARTED));
        }
        catch (RuntimeException e) {
            history.addEvent(Event.error(e.getMessage()));
        }
    }

    @Override
    public int getPort() {
        return wsServer.getPort();
    }

    @Override
    public void stop() {
        wsServer.stop();
        super.stop();
    }
}
