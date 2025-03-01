package com.aknopov.wssimulator.scenario;

/**
 * Event types for actions and reactions
 */
public enum EventType {
    STARTED,
    STOPPED,
    UPGRADE,
    OPEN,
    CLIENT_CLOSE,
    SERVER_CLOSE,
    SERVER_MESSAGE,
    CLIENT_MESSAGE,
    WAIT,
    ACTION,
    ERROR,
    IO_ERROR
}
