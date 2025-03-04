package com.aknopov.wssimulator;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Wraps an {@link InterruptedException} with an unchecked exception
 * to cancel immediately running scenario.
 */
public class ScenarioInterruptedException extends RuntimeException {
    public ScenarioInterruptedException(InterruptedException ex) {
        super(ex);
    }

    /**
     * Converts exception stack to a string
     * @return stringified stack
     */
    public String stringify()  {
        StringWriter sw = new StringWriter();
        this.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }
}
