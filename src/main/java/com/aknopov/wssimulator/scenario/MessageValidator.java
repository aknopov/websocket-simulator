package com.aknopov.wssimulator.scenario;

import com.aknopov.wssimulator.scenario.message.WebSocketMessage;

/**
 * Functional interface for validating WebSocketMessage
 */
@FunctionalInterface
public interface MessageValidator {
    boolean validate(WebSocketMessage request) throws ValidationException;
}
