package com.aknopov.wssimulator;

import javax.annotation.Nullable;

/**
 *A bunch of utility functions
 */
public final class Utils {
    private Utils() {
    }

    /**
     * Checks if boolean value is {@code true}
     * @param expression thrown if expression evaluates to {@code false}
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Checks that the specified object reference is not {@code null}.      *
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
}
