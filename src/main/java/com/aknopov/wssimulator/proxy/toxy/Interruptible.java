package com.aknopov.wssimulator.proxy.toxy;

/**
 * Interface for interrupting data exchange
 */
public interface Interruptible {
    /**
     * Interrupts immediately up- and down-stream connections.
     */
    void interrupt();
}
