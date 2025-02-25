package com.aknopov.wssimulator.scenario;

import java.lang.reflect.Executable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aknopov.wssimulator.ProtocolUpgrade;

/**
 * An interface to scenario
 */
public interface Scenario { //UC ref WebSocketEventFactory

    /**
     * Adds an act to scenario queue to wait for protocol upgrade
     *
     * @param handshakeValidator function to validate handshake that return {@code true} if validation passed
     * @param waitPeriod wait period to receive the handshake
     * @return this instance
     */
    Scenario expectProtocolUpgrade(Function<ProtocolUpgrade, Boolean> handshakeValidator, Duration waitPeriod);

    /**
     * Adds an act to scenario queue to wait for web socket opening
     *
     * @param waitPeriod maximum wait time
     * @return this instance
     */
    Scenario expectConnectionOpened(Duration waitPeriod);

    /**
     * Creates "send message" act in the scenario
     *
     * @param message message to send
     * @param initialDelay delay before sending message
     * @return this instance
     */
    Scenario sendMessage(String message, Duration initialDelay);

    /**
     * Creates "send message" act in the scenario
     *
     * @param message message to send
     * @param initialDelay delay before sending message
     * @return this instance
     */
    Scenario sendMessage(ByteBuffer message, Duration initialDelay);

    /**
     * Adds an act to the scenario queue to wait for a message from client.
     *
     * @param validator client message validator
     * @param waitPeriod wait period to receive the message
     * @return this instance
     */
    Scenario expectMessage(MessageValidator validator, Duration waitPeriod);

    /**
     * Adds an act to scenario queue to wait for web socket opening.<br/>
     * <strong>Note: Implementation library can can perform this operation at its own time.</strong>
     *
     * @param waitPeriod maximum wait time
     * @return this instance
     */
    Scenario expectConnectionClosed(Duration waitPeriod);

    /**
     * Adds an act to scenario queue to close connection
     *
     * @param statusCode status code for closing connection
     * @param initialDelay delay before closing connection
     * @return this instance
     */
    Scenario closeConnection(int statusCode, Duration initialDelay);

    /**
     * Adds an act to scenario queue to restart the server
     *
     * @param waitPeriod delay before closing connection
     *
     * @return this instance
     */
    Scenario restartServer(Duration waitPeriod);

    /**
     * Adds an act to scenario queue to perform arbitrary functionality.
     *
     * @param executable an executable to run
     * @param initialDelay delay before closing connection
     *
     * @return this instance
     */
    Scenario perform(Executable executable, Duration initialDelay);

    /**
     * Plays scenario
     *
     * @param actProcessor act processor
     */
    void play(Consumer<Act> actProcessor);

    /**
     * Checks if all acts are performed
     *
     * @return check result
     */
    boolean isDone();
}
