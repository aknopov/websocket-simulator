package com.aknopov.wssimulator.scenario;

/**
 * Don't skip documentation!
 */
//UC Should event and act types be splitted?
public enum EventType {
    STARTED,
    STOPPED,
    UPGRADE,
    OPEN,
    CLOSE,
    SERVER_MESSAGE,
    CLIENT_MESSAGE,
    WAIT,
    ACTION,
    EXPECT,
    RESTART,
    ERROR
}
