package com.aknopov.wssimulator;

import java.time.Duration;

import javax.annotation.Nullable;

/**
 * Reusable synchronization object that waits for an event that can produce an object
 *
 * @param <T> object type
 */
public class ResettableLock<T> {
    @Nullable
    private T payload;

    /**
     * Causes the current thread to wait until it is awakened.
     * Only one thread can invoke the method at a time
     *
     * @param waitDuration the maximum time to wait
     * @return the object waited for or {@code NULL} is timeout expired
     * @throws InterruptedException – if any thread interrupted the current thread
     */
    @Nullable
    public T await(Duration waitDuration) throws InterruptedException {
        payload = null;
        synchronized(this) {
            this.wait(waitDuration.toMillis());
        }
        return payload;
    }

    /**
     * Releases lock in the waiting thread and sets the payload
     */
    public void release(T payload) {
        synchronized(this) {
            this.payload = payload;
            this.notifyAll();
        }
    }
}
