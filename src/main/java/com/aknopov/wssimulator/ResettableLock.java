package com.aknopov.wssimulator;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reusable synchronization object that waits for an event that can produce an object
 *
 * @param <T> object type
 */
public class ResettableLock<T> {
    private final AtomicReference<T> refPayload = new AtomicReference<>();
    private final Class<T> dataClass;

    public ResettableLock(Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    /**
     * Causes the current thread to wait until it is awakened.
     * Only one thread can invoke the method at a time
     *
     * @param waitDuration the maximum time to wait
     * @return the object waited for or {@code NULL} is timeout expired
     * @throws InterruptedException – if any thread interrupted the current thread
     * @throws TimeoutException if data wasn't released before expiry
     */
    public T await(Duration waitDuration) throws InterruptedException {
        synchronized(this) {
            this.wait(Math.max(1, waitDuration.toMillis()));
        }
        T retVal = refPayload.getAndSet(null);
        if (retVal == null) {
            throw new TimeoutException(dataClass.getSimpleName() + " wasn't released in " + waitDuration.toMillis() + " msec");
        }
        return retVal;
    }

    /**
     * Releases lock in the waiting thread and sets the payload
     */
    public void release(T payload) {
        synchronized(this) {
            refPayload.set(payload);
            this.notifyAll();
        }
    }
}
