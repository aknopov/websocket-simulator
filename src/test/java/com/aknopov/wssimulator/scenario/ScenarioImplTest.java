package com.aknopov.wssimulator.scenario;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.Scenario;
import com.aknopov.wssimulator.ScenarioInterruptedException;
import com.aknopov.wssimulator.message.WebSocketMessage;
import jakarta.websocket.CloseReason.CloseCodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class ScenarioImplTest {

    private final static Duration TEST_DURATION = Duration.ofSeconds(123);
    private final static EventType[] ACT_TYPES = {
            EventType.UPGRADE,
            EventType.OPEN,
            EventType.SEND_MESSAGE,
            EventType.RECEIVE_MESSAGE,
            EventType.ACTION,
            EventType.SEND_MESSAGE,
            EventType.WAIT,
            EventType.DO_CLOSE,
            EventType.CLOSED
    };
    private static final int ACT_DURATION_MSEC = 50;
    private static final Duration WAIT_DURATION = Duration.ofMillis((ACT_TYPES.length + 2) * ACT_DURATION_MSEC);

    private final Scenario scenario = new ScenarioImpl();

    private void validateText(WebSocketMessage message) {
    }

    private void validateUpgrade(ProtocolUpgrade protocolUpgrade) {
    }

    private void validateCloseReason(CloseCodes closeCode) {
    }

    @BeforeEach
    void setUp() {
        scenario
                .expectProtocolUpgrade(this::validateUpgrade, TEST_DURATION)
                .expectConnectionOpened(TEST_DURATION)
                .sendMessage("Hello", TEST_DURATION)
                .expectMessage(this::validateText, TEST_DURATION)
                .perform(() -> {}, TEST_DURATION)
                .sendMessage(ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8)), TEST_DURATION)
                .wait(TEST_DURATION)
                .closeConnection(CloseCodes.PROTOCOL_ERROR, TEST_DURATION)
                .expectConnectionClosed(this::validateCloseReason, TEST_DURATION);
    }

    @Test
    void testLooping() {
        assertFalse(scenario.isDone());

        int i = 0;
        for (Act<?> act: scenario) {
            assertEquals(ACT_TYPES[i], act.eventType());
            assertEquals(TEST_DURATION, act.delay());
            i++;
        }

        assertTrue(scenario.isDone());
    }

    @Test
    void testAwaitStart() {
        assertFalse(scenario.awaitStart(Duration.ofMillis(ACT_DURATION_MSEC)));
        scenario.iterator().next();
        assertTrue(scenario.awaitStart(Duration.ofMillis(ACT_DURATION_MSEC)));
    }

    @Test
    void testRequestStop() {
        assertFalse(scenario.isDone());

        for (Act<?> act: scenario) {
            if (EventType.ACTION == act.eventType()) { // "perform" is in the middle
                scenario.requestStop();
            }
        }

        assertFalse(scenario.isDone());
    }

    @Test
    void testMarkCompletion() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(scenario::markCompletion, WAIT_DURATION.toMillis(), TimeUnit.MILLISECONDS);

        assertFalse(scenario.isDone());
        scenario.awaitCompletion(WAIT_DURATION);
        assertFalse(scenario.isDone());
    }

    @Test
    @SuppressWarnings("StatementWithEmptyBody")
    void testWaitingForCompletion() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            for (var unused : scenario) {
                // do nothing
            }
        }, ACT_DURATION_MSEC, TimeUnit.MILLISECONDS);

        assertFalse(scenario.isDone());
        scenario.awaitCompletion(WAIT_DURATION);
        assertTrue(scenario.isDone());
    }

    @Test
    void testThreadInterruptions() {
        try (MockedConstruction<CountDownLatch> latchClass = mockConstruction(CountDownLatch.class,
                (m, c) -> when(m.await(anyLong(), any(TimeUnit.class))).thenThrow(InterruptedException.class))) {
            Scenario interruptedScenario = new ScenarioImpl();

            assertThrows(ScenarioInterruptedException.class, () -> interruptedScenario.awaitStart(WAIT_DURATION));
            assertThrows(ScenarioInterruptedException.class, () -> interruptedScenario.awaitCompletion(WAIT_DURATION));
        }
    }
}