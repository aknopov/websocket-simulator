package com.aknopov.wssimulator.scenario;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.Scenario;
import com.aknopov.wssimulator.ScenarioInterruptedException;
import com.aknopov.wssimulator.WebSocketSimulator;
import com.aknopov.wssimulator.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;
import jakarta.websocket.CloseReason.CloseCode;

/**
 * Scenario implementation
 */
public class ScenarioImpl implements Scenario {
    private final Deque<Act<?>> acts;
    private final WebSocketSimulator simulator;
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch stopRequested = new CountDownLatch(1);
    private final CountDownLatch allIsDone = new CountDownLatch(1);

    public ScenarioImpl(WebSocketSimulator simulator) {
        this.simulator = simulator;
        this.acts = new ArrayDeque<>();
    }

    @Override
    public Scenario expectProtocolUpgrade(Consumer<ProtocolUpgrade> upgradeValidator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.UPGRADE, upgradeValidator));
        return this;
    }

    @Override
    public Scenario expectConnectionOpened(Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.OPEN, simulator::setEndpoint));
        return this;
    }

    @Override
    public Scenario sendMessage(String message, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.SEND_MESSAGE, () -> new TextWebSocketMessage(message)));
        return this;
    }

    @Override
    public Scenario sendMessage(ByteBuffer message, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.SEND_MESSAGE, () -> new BinaryWebSocketMessage(message)));
        return this;
    }

    @Override
    public Scenario expectMessage(Consumer<WebSocketMessage> validator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.RECEIVE_MESSAGE, validator));
        return this;
    }

    @Override
    public Scenario expectConnectionClosed(Consumer<CloseCode> validator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.CLOSED, validator));
        return this;
    }

    @Override
    public Scenario closeConnection(CloseCode closeCode, Duration initialDelay) {
        acts.add(new Act<>(initialDelay, EventType.DO_CLOSE, () -> closeCode));
        return this;
    }

    @Override
    public Scenario expectIoError(Consumer<Throwable> validator, Duration waitPeriod) {
        acts.add(new Act<>(waitPeriod, EventType.IO_ERROR, validator));
        return this;
    }

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
        started.countDown();
        try {
            while (stopRequested.getCount() > 0 && !acts.isEmpty()) {
                Act<?> next = acts.pop();
                actProcessor.accept(next);
            }
        }
        catch (ScenarioInterruptedException ex) {
            simulator.recordError("Scenario run has been interrupted: " + ex.stringify());
        }
        finally {
            allIsDone.countDown();
        }
    }

    @Override
    public void requestStop() {
        stopRequested.countDown();
    }

    @Override
    public boolean isDone() {
        return acts.isEmpty();
    }

    @Override
    public boolean awaitStart(Duration waitDuration) {
        try {
            return started.await(waitDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new ScenarioInterruptedException(e);
        }
    }

    @Override
    public boolean awaitCompletion(Duration waitDuration) {
        try {
            return allIsDone.await(waitDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new ScenarioInterruptedException(e);
        }
    }
}
