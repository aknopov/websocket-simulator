package com.aknopov.wssimulator.tyrus;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.injection.ServiceLocator;

import static org.mockito.Mockito.mock;

public class BaseTest {
    protected static final String A_PATH = "/path";
    protected static final int IDLE_SECS = 1;
    protected static final int BUFFER_SIZE = 1234;
    protected static final String TEXT_MESSAGE = "Hello!";
    protected static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(Charset.defaultCharset()));


    protected static final EventListener mockListener = mock(EventListener.class);
    protected static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);

    @BeforeEach
    protected void initInjection() {
        ServiceLocator.init(config, mockListener);
    }
}
