package com.aknopov.wssimulator;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Exception thrown when waiting for data is being expired.
 */
public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
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
