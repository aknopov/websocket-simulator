package com.aknopov.wssimulator.scenario;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Single action in a scenario that either consumes data or provides it.
 *
 * @param delay delay before performing the act or waiting for it (depending on the event type)
 * @param eventType the act type
 * @param consumer action to perform with an argument
 * @param supplier data supplier for server actions
 * @param <T> type of payload that act can produce
 */
public record Act<T>(Duration delay, EventType eventType, @Nullable Consumer<T> consumer, @Nullable Supplier<T> supplier) {
    public static final Consumer<Void> VOID_ACT = x -> {};

    public Act(Duration delay, EventType eventType, @Nullable Consumer<T> consumer) {
        this(delay, eventType, consumer, null);
    }

    public Act(Duration delay, EventType eventType, @Nullable Supplier<T> supplier) {
        this(delay, eventType, null, supplier);
    }
}
