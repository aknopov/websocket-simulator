package com.aknopov.wssimulator.jetty;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import static com.aknopov.wssimulator.SocketFactory.getAvailablePort;

/**
 * Server implementation
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final Server server;
    private final String path;
    private final SessionConfig sessionConfig;
    private final CountDownLatch started;
    private final CountDownLatch stopRequested;
    private final CountDownLatch stopped;

    /**
     * Creates server instant.
     *
     * @param host host name
     * @param path root path
     * @param sessionConfig Server configuration properties
     * @param port port to run on
     */
    public WebSocketServer(String host, String path, SessionConfig sessionConfig, int port) {
        this.port = port;
        this.server = new Server(new InetSocketAddress(host, port));
        this.path = path;
        this.sessionConfig = sessionConfig;
        this.started = new CountDownLatch(1);
        this.stopRequested = new CountDownLatch(1);
        this.stopped = new CountDownLatch(1);
        logger.debug("Created WS server on port {}", port);
    }

    /**
     * Creates server instant that will be run on some available port (see <a
     * href="com.aknopov.wssimulator.jetty.WebSocketServer#getPort()">getPort</a>).
     *
     * @param host host name
     * @param path root path
     * @param sessionConfig Session configuration properties
     */
    public WebSocketServer(String host, String path, SessionConfig sessionConfig) {
        this(host, path, sessionConfig, getAvailablePort());
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
     * @param waitDuration maximum period to wait start
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
        ServletContextHandler handler = new ServletContextHandler(path);
        server.setHandler(handler);

        EventListener eventListener = ServiceLocator.findOrCreate(EventListener.class);

        JakartaWebSocketServletContainerInitializer.configure(handler, (context, container) -> {
            ServerEndpointConfig endpointConfig =
                    ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, sessionConfig.contextPath())
                            .configurator(new ServerEndpointConfigurator(eventListener))
                            .build();
            container.setDefaultMaxTextMessageBufferSize(sessionConfig.bufferSize());
            container.setDefaultMaxSessionIdleTimeout(sessionConfig.idleTimeout().toMillis());
            container.addEndpoint(endpointConfig);
        });

        try {
            server.start();
            started.countDown();
            stopRequested.await();
            server.stop();
            stopped.countDown();
            logger.debug("WS server stopped");
        }
        catch (InterruptedException e) {
            logger.error("WS server thread interrupted", e);
        }
        catch (Exception e) {
            logger.error("Can't start server on port {}", getPort(), e);
        }
    }

    private static class ServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
        private final EventListener eventListener;

        public ServerEndpointConfigurator(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            int status = ProtocolUpgrade.SWITCH_SUCCESS_CODE;
            eventListener.onHandshake(new ProtocolUpgrade(request.getRequestURI(), request.getQueryString(),
                    request.getHeaders(), response.getHeaders(), status));
        }
    }
}
