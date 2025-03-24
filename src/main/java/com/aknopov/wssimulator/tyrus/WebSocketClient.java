package com.aknopov.wssimulator.tyrus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.Utils;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.HandshakeResponse;

/**
 * Client implementation.
 */
public class WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);
    private static final String NOT_OPENED_MESSAGE = "Websocket client is closed or not ready";

    private final URI url;
    private final EventListener eventListener;
    private final ClientEndpointConfig cec;
    @Nullable
    private WebSocketEndpoint endpoint;

    /**
     * Creates WebSocket client with an endpoint
     *
     * @param url server URL (e.g. "ws://localhost:8888/some/path")
     * @param eventListener event listener
     */
    public WebSocketClient(String url, EventListener eventListener) throws URISyntaxException {
        this(url, eventListener, HttpHeaders.of(Map.of(), (t, u) -> true));
    }

    /**
     * Creates WebSocket client with an endpoint
     *
     * @param url server URL (e.g. "ws://localhost:8888/some/path")
     * @param eventListener event listener
     * @param extraHeaders extra headers
     */
    public WebSocketClient(String url, EventListener eventListener, HttpHeaders extraHeaders)
            throws URISyntaxException {
        this.url = new URI(url);
        this.eventListener = eventListener;
        this.cec = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfigurator(extraHeaders, eventListener))
                .build();
        logger.debug("Created WS client to port {}", getPort());
    }

    /**
     * Gets port to which client connects from the constructor URL
     * @return the port
     */
    public int getPort() {
        return  url.getPort();
    }

    /**
     * Starts WebSocket client synchronously
     *
     * @return {@code true} if client started
     */
    public boolean start() {
        logger.debug("Starting WS client");

        ClientManager client = ClientManager.createClient();
        // As per https://eclipse-ee4j.github.io/tyrus-project.github.io/documentation/latest/index/tyrus-proprietary-config.html#d0e1375
        client.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 30);
        endpoint = new WebSocketEndpoint(eventListener);
        try {
            // We get session through WebSocketEndpoint
            client.connectToServer(endpoint, cec, url);
            logger.debug("Connected to server");
            return true;
        }
        catch (DeploymentException | IOException e) {
            logger.error("Can't connect to the server at {} - {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Stops web socket client and closes connection.
     */
    public void stop() {
        Utils.requireNonNull(endpoint, NOT_OPENED_MESSAGE)
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
        Utils.requireNonNull(endpoint, NOT_OPENED_MESSAGE)
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
        Utils.requireNonNull(endpoint, NOT_OPENED_MESSAGE)
                .sendBinaryMessage(message);
    }

    /**
     * Adds headers in server request.
     * (Can check response headers - needs EventListener and `afterResponse`)
     */
    private static class ClientEndpointConfigurator extends ClientEndpointConfig.Configurator {
        private final HttpHeaders clientHeaders;
        private final Map<String, List<String>> allHeaders;
        private final EventListener eventListener;

        ClientEndpointConfigurator(HttpHeaders clientHeaders, EventListener eventListener) {
            this.clientHeaders = clientHeaders;
            this.eventListener = eventListener;
            this.allHeaders = new HashMap<>();
        }

        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            headers.putAll(clientHeaders.map());
            allHeaders.putAll(headers);
        }

        @Override
        public void afterResponse(HandshakeResponse hr) {
            ProtocolUpgrade handshake = new ProtocolUpgrade(URI.create("."), "", allHeaders, hr.getHeaders(), 0);
            eventListener.onHandshake(handshake);
        }
    }
}
