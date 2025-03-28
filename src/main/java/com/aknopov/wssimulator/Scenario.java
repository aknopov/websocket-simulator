package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;

import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.scenario.Act;
import jakarta.websocket.CloseReason.CloseCodes;

/**
 * An interface to scenario
 */
public interface Scenario extends Iterable<Act<?>> {

    /**
     * Adds an act to scenario queue to wait for protocol upgrade (server-specific)
     *
     * @param upgradeValidator  protocol upgrade validator that throws {@code ValidationException} if failed
     * @param waitPeriod wait period to receive the handshake
     * @return this instance
     */
    Scenario expectProtocolUpgrade(Consumer<ProtocolUpgrade> upgradeValidator, Duration waitPeriod);

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
     * Adds an act to the scenario queue to wait for incoming message.
     *
     * @param validator client message validator
     * @param waitPeriod wait period to receive the message
     * @return this instance
     */
    Scenario expectMessage(Consumer<WebSocketMessage> validator, Duration waitPeriod);

    /**
     * Adds an act to scenario queue to wait for websocket close.<br/>
     *
     * @param validator close reason validator
     * @param waitPeriod maximum wait time
     * @return this instance
     */
    Scenario expectConnectionClosed(Consumer<CloseCodes> validator, Duration waitPeriod);

    /**
     * Adds an act to scenario queue to close connection.
     *
     * @param closeCode code for closing connection
     * @param initialDelay delay before closing connection
     * @return this instance
     */
    Scenario closeConnection(CloseCodes closeCode, Duration initialDelay);

    /**
     * Adds an act to scenario queue to perform arbitrary functionality.
     *
     * @param runnable code to run
     * @param initialDelay delay before closing connection
     * @return this instance
     */
    Scenario perform(Runnable runnable, Duration initialDelay);

    /**
     * Waits specified time
     *
     * @param waitPeriod wait period
     * @return this instance
     */
    Scenario wait(Duration waitPeriod);

    /**
     * Requests to stop executing scenario
     */
    void requestStop();

    /**
     * Waits for scenario to be started
     *
     * @param waitDuration wait duration
     * @return {@code true} if scenario started before wait expiry
     */
    boolean awaitStart(Duration waitDuration);

    /**
     * Checks if all acts were performed in the scenario
     *
     * @return check result
     */
    boolean isDone();

    /**
     * Sets flag that scenario was finished or interrupted.
     */
    void markCompletion();

    /**
     * Waits for scenario to be completed
     *
     * @param waitDuration wait duration
     * @return {@code true} if scenario completed before wait expiry
     * @throws TimeoutException if wait was interrupted
     */
    boolean awaitCompletion(Duration waitDuration);
}
