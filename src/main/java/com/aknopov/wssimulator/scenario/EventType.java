package com.aknopov.wssimulator.scenario;

/**
 * Event types for actions and reactions
 */
public enum EventType {
    STARTED,
    STOPPED,
    UPGRADE,
    OPEN,
    CLOSED,
    DO_CLOSE,
    SEND_MESSAGE,
    RECEIVE_MESSAGE,
    WAIT,
    ACTION,
    ERROR,
    IO_ERROR
}
