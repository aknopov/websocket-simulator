package com.aknopov.wssimulator.scenario;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.WebSocketSimulator;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import jakarta.websocket.CloseReason;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ScenarioImplTest {

    private final static Duration TEST_DURATION = Duration.ofSeconds(123);
    private final static EventType[] ACT_TYPES = {
            EventType.UPGRADE,
            EventType.OPEN,
            EventType.SERVER_MESSAGE,
            EventType.CLIENT_MESSAGE,
            EventType.ACTION,
            EventType.SERVER_MESSAGE,
            EventType.IO_ERROR,
            EventType.WAIT,
            EventType.SERVER_CLOSE,
            EventType.CLIENT_CLOSE
    };

    private final WebSocketSimulator mockSimulator = mock(WebSocketSimulator.class);
    private final Scenario scenario = new ScenarioImpl(mockSimulator);

    private void validateText(WebSocketMessage message) {
    }

    private void validateUpgrade(ProtocolUpgrade protocolUpgrade) {
    }

    private void validateCloseReason(CloseReason.CloseCode closeCode) {
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
                .expectIoError(Throwable::getCause, TEST_DURATION)
                .wait(TEST_DURATION)
                .closeConnection(CloseReason.CloseCodes.PROTOCOL_ERROR, TEST_DURATION)
                .expectConnectionClosed(this::validateCloseReason, TEST_DURATION);
    }

    @Test
    void testPlaying() {
        assertFalse(scenario.isDone());

        AtomicInteger idx = new AtomicInteger();
        scenario.play((act) -> {
            int i = idx.getAndIncrement();
            assertEquals(ACT_TYPES[i], act.eventType());
            assertEquals(TEST_DURATION, act.delay());
        });

        assertTrue(scenario.isDone());
    }

    @Test
    void testRequestStop() {
        assertFalse(scenario.isDone());

        AtomicInteger idx = new AtomicInteger();
        scenario.play((act) -> {
            int i = idx.getAndIncrement();
            if (i == 5) {
                scenario.requestStop();
            }
        });

        assertFalse(scenario.isDone());
    }
}