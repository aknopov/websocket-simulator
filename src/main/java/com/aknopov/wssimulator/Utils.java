package com.aknopov.wssimulator;

import java.time.Duration;

import javax.annotation.Nullable;

/**
 * A bunch of utility functions
 */
public final class Utils {
    private Utils() {
    }

    /**
     * Checks an expectation throwing {@link IllegalArgumentException} if check fails.
     * 
     * @param check check value
     * @param errMessage message in the exception
     */
    public static void checkArgument(boolean check, String errMessage) {
        if (!check) {
            throw new IllegalArgumentException(errMessage);
        }
    }

    /**
     * Checks an expectation throwing {@link IllegalStateException} if check fails.
     *
     * @param check check value
     * @param errMessage message in the exception
     */
    public static void checkState(boolean check, String errMessage) {
        if (!check) {
            throw new IllegalStateException(errMessage);
        }
    }

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * @param obj the object reference to check for nullity
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws IllegalStateException if {@code obj} is {@code null}
     */
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public static <T> T requireNonNull(@Nullable T obj) {
        if (obj == null) {
            throw new IllegalStateException();
        }
        return obj;
    }

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * @param obj the object reference to check for nullity
     * @param <T> the type of the reference
     * @param message message to use in exception
     * @return {@code obj} if not {@code null}
     * @throws IllegalStateException if {@code obj} is {@code null}
     */
    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public static <T> T requireNonNull(@Nullable T obj, String message) {
        if (obj == null) {
            throw new IllegalStateException(message);
        }
        return obj;
    }

    /**
     * Sleeps specified time and ignores {@link InterruptedException}
     *
     * @param sleepMs sleep duration in milliseconds
     */
    public static void sleepUnchecked(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        }
        catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Sleeps specified time and ignores {@link InterruptedException}
     *
     * @param sleepTime sleep duration
     */
    public static void sleepUnchecked(Duration sleepTime) {
        sleepUnchecked(sleepTime.toMillis());
    }
}
