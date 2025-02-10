package com.aknopov.wssimulator;

import java.time.Duration;

/**
 * Configuration for creating WebSocket endpoint
 */

/**
 * Configuration for creating WebSocket endpoint
 *
 * @param path server endpoint path
 * @param idleTimeout connection idle timeout
 * @param bufferSize read buffer size (message max size)
 */
public record SessionConfig(String path, Duration idleTimeout, int bufferSize) {
}
