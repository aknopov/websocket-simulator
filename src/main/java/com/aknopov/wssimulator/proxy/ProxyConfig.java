package com.aknopov.wssimulator.proxy;

import java.time.Duration;

/**
 * Socket proxy configuration
 *
 * @param downPort proxy downstream (incoming) port on localhost
 * @param upPort proxy upstream (outgoing) port on localhost
 * @param soTimeout socket idle timeout (SO_TIMEOUT)
 * @param bufSize proxy buffer size
 * @param shutdownTime maximum time after which proxy shuts down
 */
public record ProxyConfig(int downPort, int upPort, int soTimeout, int bufSize, Duration shutdownTime) {
}
