package com.aknopov.wssimulator.tyrus;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.DeploymentException;

import static com.aknopov.wssimulator.SocketFactory.getAvailablePort;

/**
 * Server implementation
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final Server server;
    private final CountDownLatch stopLatch;
    private final CountDownLatch startLatch;

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
        this.server = new Server(host, port, path, properties, WebSocketEndpoint.getConfigClass());
        this.stopLatch = new CountDownLatch(1);
        this.startLatch = new CountDownLatch(1);
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
        if (startLatch.getCount() == 0) {
            throw new IllegalStateException("Server is neither in initial state nor stopped");
        }
        logger.debug("Starting WS server on port {}", port);
        new Thread(this::runServer).start();
    }

    /**
     * Stops the server and closes connection.
     */
    public synchronized void stop() {
        if (stopLatch.getCount() == 1) {
            logger.debug("Stopping WS server on port {}", port);
            stopLatch.countDown();
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
            return startLatch.await(waitDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            logger.error("Interrupted while waiting for server port number", e);
        }
        return false;
    }

    private void runServer() {
        try {
            server.start();
            startLatch.countDown();
            stopLatch.await();
            logger.debug("WS server stopped");
        }
        catch (DeploymentException e) {
            logger.error("Can't start server on port {}", server.getPort(), e);
        }
        catch (InterruptedException e) {
            logger.error("WS server thread interrupted", e);
        }
    }
}
