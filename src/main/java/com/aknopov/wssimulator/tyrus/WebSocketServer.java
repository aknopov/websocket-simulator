package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import static com.aknopov.wssimulator.SocketFactory.getAvailablePort;

/**
 * Server implementation
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final Server server;
    private final CountDownLatch started;
    private final CountDownLatch stopRequested;
    private final CountDownLatch stopped;

    /**
     * Creates server instant.
     *
     * @param host host name
     * @param path root path
     * @param properties Tyrus server configuration properties
     * @param port port to run on
     */
    public WebSocketServer(String host, String path, Map<String, Object> properties, int port) {
        this.port = port;
        this.server = new Server(host, port, path, properties, MyServerApplicationConfig.class);
        this.started = new CountDownLatch(1);
        this.stopRequested = new CountDownLatch(1);
        this.stopped = new CountDownLatch(1);
        logger.debug("Created WS server on port {}", port);
    }

    /**
     * Creates server instant that will be run on some available port (see <a href="com.aknopov.wssimulator.tyrus.WebSocketServer#getPort()">getPort</a>).
     *
     * @param host host name
     * @param path root path
     * @param properties Tyrus server configuration properties
     */
    public WebSocketServer(String host, String path, Map<String, Object> properties) {
        this(host, path, properties, getAvailablePort());
    }

    /**
     * Starts WebSocket server asynchronously
     *
     * @throws IllegalStateException from creating and running the client
     */
    public void start() {
        if (started.getCount() == 0) {
            throw new IllegalStateException("Server is neither in initial state nor stopped");
        }
        logger.debug("Starting WS server on port {}", port);
        new Thread(this::runServer).start();
    }

    /**
     * Stops the server and closes connection.
     */
    public synchronized void stop() {
        if (stopRequested.getCount() == 1) {
            logger.debug("Stopping WS server on port {}", port);
            stopRequested.countDown();
        }
    }

    /**
     * Gets the port on which server is running.
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Waits till server starts.
     *
     * @param waitDuration maximum period to wait for start
     * @return {@code true} if server started before expiry
     */
    public boolean waitForStart(Duration waitDuration) {
        try {
            return started.await(waitDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            logger.error("Interrupted while waiting for server port number", e);
        }
        return false;
    }

    /**
     * Waits till server stops.
     *
     * @param waitDuration maximum period to wait for stop
     * @return {@code true} if server stopped before expiry
     */
    public boolean waitForStop(Duration waitDuration) {
        try {
            return stopped.await(waitDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            logger.error("Interrupted while waiting for server stop", e);
        }
        return false;
    }

    private void runServer() {
        try {
            server.start();
            started.countDown();
            stopRequested.await();
            server.stop();
            stopped.countDown();
            logger.debug("WS server on port {} stopped", getPort());
        }
        catch (DeploymentException e) {
            logger.error("Can't start server on port {}", server.getPort(), e);
        }
        catch (InterruptedException e) {
            logger.error("WS server thread interrupted", e);
        }
    }

    /**
     * Tyrus requires this class to be public!
     */
    public static class MyServerApplicationConfig implements ServerApplicationConfig {
        private final EventListener eventListener;
        private final SessionConfig sessionConfig;

        public MyServerApplicationConfig() {
            eventListener = ServiceLocator.findOrCreate(EventListener.class);
            sessionConfig = ServiceLocator.findOrCreate(SessionConfig.class);
        }

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
            return Set.of(ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, sessionConfig.contextPath())
                    .configurator(new ServerEndpointConfigurator(eventListener))
                    .build());
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
            return Set.of();
        }
    }

    private static class ServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
        private final EventListener eventListener;

        public ServerEndpointConfigurator(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            int status = (response instanceof TyrusUpgradeResponse tyrusResponse) ? tyrusResponse.getStatus() : -1;
            eventListener.onHandshake(new ProtocolUpgrade(request.getRequestURI(), request.getQueryString(),
                    request.getHeaders(), response.getHeaders(), status));
        }
    }
}
