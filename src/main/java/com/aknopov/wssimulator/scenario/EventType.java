package com.aknopov.wssimulator.scenario;

/**
 * Don't skip documentation!
 */
//UC Should event and act types be split?
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
    RESTART,
    ERROR,
    IO_ERROR
}
