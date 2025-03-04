package com.aknopov.wssimulator;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.tyrus.WebSocketClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

class WebSocketClientSimulatorTest {

    @Test
    void testInvalidUri() {
        assertThrows(IllegalArgumentException.class, () -> new WebSocketClientSimulator("://@example.com"));
    }

    @Test
    void testGetPort() {
        WebSocketClientSimulator simulator = new WebSocketClientSimulator("ws://localhost:12345/path");

        assertEquals(12345, simulator.getPort());
    }

    @Test
    void testLiveCycle() {
        try (MockedConstruction<WebSocketClient> clientMockClass = mockConstruction(WebSocketClient.class)) {
            WebSocketClientSimulator simulator = new WebSocketClientSimulator("ws://localhost:12345/path");
            simulator.start();
            simulator.awaitScenarioCompletion(Duration.ofMillis(10));
            simulator.stop();

            List<WebSocketClient> instances = clientMockClass.constructed();
            assertEquals(1, instances.size());
            WebSocketClient wsClient = instances.get(0);
            verify(wsClient).start();
            verify(wsClient).stop();

            List<Event> events = simulator.getHistory();
            assertEquals(2, events.size());
            assertEquals(EventType.STARTED, events.get(0).eventType());
            assertEquals(EventType.STOPPED, events.get(1).eventType());
        }
    }
}