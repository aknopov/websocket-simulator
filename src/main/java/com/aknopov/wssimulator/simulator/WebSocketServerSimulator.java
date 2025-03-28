package com.aknopov.wssimulator.simulator;

import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.TimeoutException;
import com.aknopov.wssimulator.injection.ServiceLocator;
import com.aknopov.wssimulator.jetty.WebSocketServer;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;

/**
 * Implementation of WebSocketSimulator for the server
 */
public class WebSocketServerSimulator extends WebSocketSimulatorBase {
    public static final int DYNAMIC_PORT = 0;
    private final WebSocketServer wsServer;
    private final SessionConfig config;

    /**
     * Creates simulator with given configuration
     *
     * @param config session configuration
     * @param port port number, 0 - is for dynamic
     */
    public WebSocketServerSimulator(SessionConfig config, int port) {
        this(config, port != DYNAMIC_PORT
                ? new WebSocketServer("localhost", "/", config, port)
                : new WebSocketServer("localhost", "/", config));
    }

    //VisibleForTesting
    WebSocketServerSimulator(SessionConfig config, WebSocketServer wsServer) {
        super("ServerSimulator");
        this.wsServer = wsServer;
        this.config = config;
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
            recordError(e.getMessage());
        }
    }

    @Override
    public int getPort() {
        return wsServer.getPort();
    }

    @Override
    public void stop() {
        super.stop();
        wsServer.stop();
        wsServer.waitForStop(config.idleTimeout());
    }
}
