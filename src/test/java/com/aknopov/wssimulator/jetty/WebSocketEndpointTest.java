package com.aknopov.wssimulator.jetty;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.SimulatorEndpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketEndpointTest extends BaseTest {
    @Test
    void testCreation() {
        WebSocketEndpoint serverEndpoint = new WebSocketEndpoint();

        serverEndpoint.onOpen(mock(Session.class), mock(EndpointConfig.class));
        assertTrue(serverListener.openHappened());

        EventListener mockListener = mock(EventListener.class);
        WebSocketEndpoint clientEndpoint = new WebSocketEndpoint(mockListener);
        clientEndpoint.onOpen(mock(Session.class), mock(EndpointConfig.class));
        verify(mockListener).onOpen(any(SimulatorEndpoint.class), anyMap());
    }
}