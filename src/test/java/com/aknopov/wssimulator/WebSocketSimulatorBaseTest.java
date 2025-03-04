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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        List<Event> events = simulator.getHistory();
        assertEquals(1, events.size());
        assertEquals(EventType.SEND_MESSAGE, events.get(0).eventType());
        assertEquals(TEXT_MESSAGE, events.get(0).description());
    }


    @Test
    void testSendBinaryMessage() {
        simulator.setEndpoint(mockEndpoint);

        simulator.sendMessage(new BinaryWebSocketMessage(BINARY_MESSAGE));

        verify(mockEndpoint).sendBinaryMessage(BINARY_MESSAGE);
        List<Event> events = simulator.getHistory();
        assertEquals(1, events.size());
        assertEquals(EventType.SEND_MESSAGE, events.get(0).eventType());
        assertEquals("Binary, len=" + BINARY_MESSAGE.remaining(), events.get(0).description());
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
    void testStart() {
        assertNotNull(simulator.scenarioThread);
        assertFalse(simulator.scenarioThread.isAlive());

        simulator.start();

        assertTrue(simulator.scenarioThread.isAlive());
    }

    @Test
    void testStop() {
        simulator.getScenario()
                .wait(TEST_WAIT);
        simulator.start();

        simulator.getScenario().awaitStart(TEST_WAIT);
        simulator.stop();
        Helpers.sleepUninterrupted(TEST_WAIT.toMillis());

        List<Event> errors = simulator.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).description().startsWith("Scenario run interrupted:"));
    }

    @Test
    void testWait() {
        simulator.getScenario()
                .wait(TEST_WAIT);
        simulator.start();

        Helpers.sleepUninterrupted(TEST_WAIT.toMillis() * 2);
        assertTrue(simulator.isScenarioDone());

        List<Event> events = simulator.getHistory();
        assertEquals(1, events.size());
        assertEquals(EventType.WAIT, events.get(0).eventType());
    }

    @Test
    void testWaitFor() {
        simulator.getScenario()
                .expectConnectionOpened(TEST_WAIT);
        simulator.start();

        Helpers.sleepUninterrupted(TEST_WAIT.toMillis() * 2);
        assertTrue(simulator.isScenarioDone());

        List<Event> errors = simulator.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).description().startsWith("Data wasn't released in 200 msec"));
    }
}