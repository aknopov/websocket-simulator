package com.aknopov.wssimulator.tyrus;

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