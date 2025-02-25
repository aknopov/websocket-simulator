package com.aknopov.wssimulator.scenario;

import java.lang.reflect.Executable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aknopov.wssimulator.ProtocolUpgrade;

/**
 * Don't skip documentation!
 */
public class ScenarioImpl implements Scenario {
    @Override
    public Scenario expectProtocolUpgrade(Function<ProtocolUpgrade, Boolean> handshakeValidator, Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario expectConnectionOpened(Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario sendMessage(String message, Duration initialDelay) {
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

    @Override
    public Scenario restartServer(Duration waitPeriod) {
        return this;
    }

    @Override
    public Scenario perform(Executable executable, Duration initialDelay) {
        return this;
    }

    @Override
    public void play(Consumer<Act> actProcessor) {
        //UC
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
