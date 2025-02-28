package com.aknopov.wssimulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {
    @Test
    void testCheckArgument() {
        assertDoesNotThrow(() -> Utils.checkArgument(true));
        assertThrows(IllegalStateException.class, () -> Utils.checkArgument(false));
    }

    @Test
    void testRequireNonNull() {
        assertDoesNotThrow(() -> Utils.requireNonNull(new Object()));
        assertThrows(IllegalStateException.class, () -> Utils.requireNonNull(null));
    }
}