package com.aknopov.wssimulator.scenario;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.WebSocketSimulator;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import jakarta.websocket.CloseReason.CloseCode;

/**
 * Scenario implementation
 */
public class ScenarioImpl implements Scenario {
    private final Deque<Act<?>> acts;
    private final WebSocketSimulator simulator;

    public ScenarioImpl(WebSocketSimulator simulator) {
        this.simulator = simulator;
        this.acts = new ArrayDeque<>();
    }

    @Override
    public Scenario expectProtocolUpgrade(Consumer<ProtocolUpgrade> upgradeValidator, Duration waitPeriod) {
        acts.push(new Act<>(waitPeriod, EventType.UPGRADE, upgradeValidator));
        return this;
    }

    @Override
    public Scenario expectConnectionOpened(Duration waitPeriod) {
        acts.push(new Act<>(waitPeriod, EventType.OPEN, simulator::setEndpoint));
        return this;
    }

    @Override
    public Scenario sendMessage(String message, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.SERVER_MESSAGE, () -> new TextWebSocketMessage(message)));
        return this;
    }

    @Override
    public Scenario sendMessage(ByteBuffer message, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.SERVER_MESSAGE, () -> new BinaryWebSocketMessage(message)));
        return this;
    }

    @Override
    public Scenario expectMessage(Consumer<WebSocketMessage> validator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.CLIENT_MESSAGE, validator));
        return this;
    }

    @Override
    public Scenario expectConnectionClosed(Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.CLIENT_CLOSE, Act.VOID_ACT));
        return this;
    }

    @Override
    public Scenario closeConnection(CloseCode closeCode, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.SERVER_CLOSE, () -> closeCode));
        return this;
    }

    @Override
    public Scenario expectIoError(Consumer<Throwable> validator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.IO_ERROR, Act.VOID_ACT));
        return this;
    }

//TODO    @Override
//    public Scenario restartServer(Duration waitPeriod) {
//        return this;
//    }

    @Override
    public Scenario perform(Runnable runnable, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.ACTION, x -> runnable.run()));
        return this;
    }

    @Override
    public Scenario wait(Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.WAIT, Act.VOID_ACT));
        return this;
    }

    @Override
    public void play(Consumer<Act<?>> actProcessor) {
        while (!acts.isEmpty()) {
            Act<?> next = acts.pop();
            actProcessor.accept(next);
        }
    }

    @Override
    public boolean isDone() {
        return acts.isEmpty();
    }
}
