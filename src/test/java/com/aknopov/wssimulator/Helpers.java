package com.aknopov.wssimulator;

/**
 * Test aids
 */
public final class Helpers {
    private Helpers() {
    }

    public static void sleepUninterrupted(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
        }
    }
}
