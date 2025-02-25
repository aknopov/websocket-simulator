package com.aknopov.wssimulator.scenario;

import java.lang.reflect.Executable;
import java.time.Duration;

/**
 * Single action in a scenario
 *
 * @param delay delay before performing ac
 * @param eventType the act type
 * @param executable action to perform
 */
public record Act(Duration delay, EventType eventType, Executable executable) {
}
