package com.aknopov.wssimulator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebSocketEndpointTest extends BaseTest {
    @Test
    void testConfigCreation() throws Exception {
        var configClass = WebSocketEndpoint.getConfigClass();
        var obj = configClass.getDeclaredConstructor()
                .newInstance();
        assertNotNull(obj);
    }
}