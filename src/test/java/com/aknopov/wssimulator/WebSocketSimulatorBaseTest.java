package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketSimulatorBaseTest {
    private static final String TEXT_MESSAGE = "Hello!";
    private static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(StandardCharsets.UTF_8));
    private static final Duration TEST_WAIT = Duration.ofMillis(200);

    private static class TestWebSocketSimulator extends WebSocketSimulatorBase {
        protected TestWebSocketSimulator() {
            super("Test");
        }

        @Override
        public int getPort() {
            return -1;
        }

        @Override
        public void start() {
            scenarioThread.start();
            history.addEvent(Event.create(EventType.STARTED));
        }

        @Override
        public void stop() {
            scenario.requestStop();
            history.addEvent(Event.create(EventType.STOPPED));
        }
    }

    private final SimulatorEndpoint mockEndpoint = mock(SimulatorEndpoint.class);
    private final TestWebSocketSimulator simulator = new TestWebSocketSimulator();

    @Test
    void testGetScenario() {
        assertNotNull(simulator.getScenario());
    }

    @Test
    void testSendTextMessage() {
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new TextWebSocketMessage(TEXT_MESSAGE));

        verify(mockEndpoint).sendTextMessage(TEXT_MESSAGE);
        List<Event> events = simulator.getHistory().getEvents();
        assertEquals(1, events.size());
        assertEquals(EventType.SEND_MESSAGE, events.get(0).eventType());
        assertEquals("Text message", events.get(0).description());
    }


    @Test
    void testSendBinaryMessage() {
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new BinaryWebSocketMessage(BINARY_MESSAGE));

        verify(mockEndpoint).sendBinaryMessage(BINARY_MESSAGE);
        List<Event> events = simulator.getHistory().getEvents();
        assertEquals(1, events.size());
        assertEquals(EventType.SEND_MESSAGE, events.get(0).eventType());
        assertEquals("Binary message", events.get(0).description());
    }

    @Test
    void testSendMessageFailure() {
        doThrow(IllegalStateException.class).when(mockEndpoint).sendTextMessage(anyString());
        doThrow(UncheckedIOException.class).when(mockEndpoint).sendBinaryMessage(any(ByteBuffer.class));
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new TextWebSocketMessage(TEXT_MESSAGE));
        simulator.sendMessage(new BinaryWebSocketMessage(BINARY_MESSAGE));

        List<Event> errors = simulator.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Attempted to send text message before establishing connection", errors.get(0).description());
        assertEquals("Can't send binary: null", errors.get(1).description());
    }

    @ParameterizedTest
    @ValueSource(classes = { NullPointerException.class, TimeoutException.class, ValidationException.class,
            UncheckedIOException.class })
    void testProcessFailures(Class<? extends Exception> exceptionClass) {
        Runnable mockRunnable = mock(Runnable.class);
        doThrow(exceptionClass).when(mockRunnable).run();

        assertDoesNotThrow(() -> simulator.process(mockRunnable));

        assertEquals(1, simulator.getErrors().size());;
    }

    @Test
    void testWait() {
        simulator.getScenario()
                .wait(TEST_WAIT);
        simulator.start();

        Helpers.sleepUninterrupted(TEST_WAIT.toMillis() / 2);
        simulator.getThread().interrupt();

        simulator.stop();
        simulator.scenario.awaitCompletion(TEST_WAIT);

        List<Event> errors = simulator.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).description().startsWith("Expected action didn't happen at"));
    }

    @Test
    void testWaitFor() {
        simulator.getScenario()
                .expectConnectionOpened(TEST_WAIT);
        simulator.start();

        Helpers.sleepUninterrupted(TEST_WAIT.toMillis() / 2);
        simulator.getThread().interrupt();

        simulator.stop();
        simulator.scenario.awaitCompletion(TEST_WAIT);

        List<Event> errors = simulator.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).description().startsWith("Expected action didn't happen at"));
    }
}