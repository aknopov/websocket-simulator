package com.aknopov.wssimulator.scenario;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Single action in a scenario
 *
 * @param delay delay before performing the act
 * @param eventType the act type
 * @param consumer action to perform with an argument
 */
public record Act(Duration delay, EventType eventType, Consumer<?> consumer) {
    public static final Consumer<Void> NO_ACT = x -> {};
}
