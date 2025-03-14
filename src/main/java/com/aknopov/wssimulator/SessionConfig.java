package com.aknopov.wssimulator;

import java.time.Duration;

/**
 * Configuration for creating WebSocket endpoint
 *
 * @param contextPath server endpoint context path
 * @param idleTimeout connection idle timeout
 * @param bufferSize read buffer size (message max size)
 */
public record SessionConfig(String contextPath, Duration idleTimeout, int bufferSize) {
    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(60);
    private static final int BUFFER_SIZE = 1024;

    public SessionConfig(String contextPath) {
        this(contextPath, IDLE_TIMEOUT, BUFFER_SIZE);
    }
}
