package com.aknopov;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Don't skip documentation!
 *
 * @author sasha
 */
class WebSocketEndpoint2Test {

    @Test
    void testConfigCreation() throws Exception
    {
        var configClass = WebSocketEndpoint2.getConfigClass();
        var obj = configClass.getDeclaredConstructor().newInstance();
        assertNotNull(obj);
    }

}