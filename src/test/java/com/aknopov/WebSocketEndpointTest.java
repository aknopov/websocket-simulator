package com.aknopov;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Don't skip documentation!
 *
 * @author sasha
 */
class WebSocketEndpointTest {

    @Test
    void testConfigCreation() throws Exception
    {
        var configClass = WebSocketEndpoint.getConfigClass();
        var obj = configClass.getDeclaredConstructor().newInstance();
        assertNotNull(obj);
    }

}