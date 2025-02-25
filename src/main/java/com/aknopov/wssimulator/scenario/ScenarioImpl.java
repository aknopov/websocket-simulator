package com.aknopov.wssimulator.scenario;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.WebSocketSimulator;

/**
 * Don't skip documentation!
 */
public class ScenarioImpl implements Scenario {

    private final Deque<Act> acts;
    private final WebSocketSimulator simulator;

    public ScenarioImpl(WebSocketSimulator simulator) {
        this.simulator = simulator;
        this.acts = new ArrayDeque<>();
    }

    @Override
    public Scenario expectProtocolUpgrade(Consumer<ProtocolUpgrade> upgradeValidator, Duration waitPeriod) {
        acts.push(new Act(waitPeriod, EventType.UPGRADE, upgradeValidator));
        return this;
    }

    @Override
    public Scenario expectConnectionOpened(Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario sendMessage(String message, Duration initialDelay) {
        acts.add(new Act(initialDelay, EventType.ACTION, m -> simulator.sendTextMessage(message)));
        return this;
    }

    @Override
    public Scenario sendMessage(ByteBuffer message, Duration initialDelay) {
        return this;
    }

    @Override
    public Scenario expectMessage(MessageValidator validator, Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario expectConnectionClosed(Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario closeConnection(int statusCode, Duration initialDelay) {
        return this;
    }

//    @Override
//    public Scenario restartServer(Duration waitPeriod) {
//        return this;
//    }

    @Override
    public Scenario perform(Runnable runnable, Duration initialDelay) {
        return this;
    }

    @Override
    public void play(Consumer<Act> actProcessor) {
        while (!acts.isEmpty()) {
            Act next = acts.pop();
            actProcessor.accept(next);
        }
    }

    @Override
    public boolean isDone() {
        return acts.isEmpty();
    }
}
