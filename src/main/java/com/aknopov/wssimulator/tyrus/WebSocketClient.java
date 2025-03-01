package com.aknopov.wssimulator.tyrus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.Utils;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.DeploymentException;

/**
 * Client implementation.
 */
public class WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private final URI url;
    private final ClientEndpointConfig cec;
    @Nullable
    private WebSocketEndpoint endpoint;

    /**
     * Creates WebSocket client with an endpoint
     *
     * @param url server URL (e.g. "ws://localhost:8888/some/path")
     */
    public WebSocketClient(String url) throws URISyntaxException {
        this.url = new URI(url);
        this.cec = ClientEndpointConfig.Builder.create()
                .build();
    }

    /**
     * Starts WebSocket client synchronously
     *
     * @return {@code true} if client started
     */
    public boolean start() {
        logger.debug("Starting WS client");

        ClientManager client = ClientManager.createClient();
        endpoint = new WebSocketEndpoint();
        try {
            // We get session through WebSocketEndpoint
            client.connectToServer(endpoint, cec, url);
            return true;
        }
        catch (DeploymentException | IOException e) {
            logger.error("Can't connect to the server at {}", url, e);
            return false;
        }
    }

    /**
     * Stops web socket client and closes connection.
     */
    public void stop() {
        assumeConnected();
        Utils.requireNonNull(endpoint)
                .closeConnection(CloseCodes.NORMAL_CLOSURE);
        endpoint = null;
    }

    /**
     * Sends text message to the server
     *
     * @param message the message
     *
     * @throws IllegalStateException if connection is not opened
     */
    public void sendTextMessage(String message) {
        assumeConnected();
        Utils.requireNonNull(endpoint)
                .sendTextMessage(message);
    }

    /**
     * Sends binary message to the server
     *
     * @param message the message
     *
     * @throws IllegalStateException if connection is not opened
     */
    public void sendBinaryMessage(ByteBuffer message) {
        assumeConnected();
        Utils.requireNonNull(endpoint)
                .sendBinaryMessage(message);
    }

    // Don't want to throw NPE from `Objects.requireNonNull`
    private void assumeConnected() {
        if (endpoint == null) {
            throw new IllegalStateException("Websocket client is closed or not ready");
        }
    }
}
