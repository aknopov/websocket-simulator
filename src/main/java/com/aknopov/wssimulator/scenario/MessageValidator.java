package com.aknopov.wssimulator.scenario;

import com.aknopov.wssimulator.scenario.message.WebSocketMessage;

/**
 * Functional interface for validating WebSocketMessage
 */
@FunctionalInterface
public interface MessageValidator {
    /**
     * Checks message validity
     *
     * @param message the message
     * @throws ValidationException if validation failed
     */
    void validate(WebSocketMessage message);
}
