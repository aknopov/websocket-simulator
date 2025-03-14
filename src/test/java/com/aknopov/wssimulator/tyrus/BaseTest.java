package com.aknopov.wssimulator.tyrus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.SessionConfig;
import com.aknopov.wssimulator.SimulatorEndpoint;
import com.aknopov.wssimulator.injection.ServiceLocator;
import jakarta.websocket.CloseReason;

import static org.mockito.Mockito.mock;

public class BaseTest {
    protected static final String A_PATH = "/path";
    protected static final int IDLE_SECS = 30;
    protected static final int BUFFER_SIZE = 1234;
    protected static final String TEXT_MESSAGE = "Hello!";
    protected static final ByteBuffer BINARY_MESSAGE =
            ByteBuffer.wrap("Binary message".getBytes(StandardCharsets.UTF_8));


    protected final TestEventListener serverListener = new TestEventListener();
    protected static final SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);

    @BeforeEach
    protected void initInjection() {
        ServiceLocator.init(config, serverListener);
    }

    protected static class TestEventListener implements EventListener {
        final CountDownLatch handshakeEvent = new CountDownLatch(1);
        final CountDownLatch openEvent = new CountDownLatch(1);
        final CountDownLatch closeEvent = new CountDownLatch(1);
        final CountDownLatch errorEvent = new CountDownLatch(1);
        final CountDownLatch textEvent = new CountDownLatch(1);
        final CountDownLatch binaryEvent = new CountDownLatch(1);

        @Override
        public void onHandshake(ProtocolUpgrade handshake) {
            handshakeEvent.countDown();
        }

        @Override
        public void onOpen(SimulatorEndpoint endpoint, Map<String, Object> context) {
            openEvent.countDown();
        }

        @Override
        public void onClose(CloseReason.CloseCode closeCode) {
            closeEvent.countDown();
        }

        @Override
        public void onError(Throwable error) {
            errorEvent.countDown();
        }

        @Override
        public void onTextMessage(String message) {
            textEvent.countDown();
        }

        @Override
        public void onBinaryMessage(ByteBuffer message) {
            binaryEvent.countDown();
        }

        boolean waitForHandshake(Duration waitTime) {
            return waitForEvent(handshakeEvent, waitTime);
        }

        boolean waitForOpen(Duration waitTime) {
            return waitForEvent(openEvent, waitTime);
        }

        boolean waitForClose(Duration waitTime) {
            return waitForEvent(closeEvent, waitTime);
        }

        boolean waitForBinary(Duration waitTime) {
            return waitForEvent(binaryEvent, waitTime);
        }

        boolean waitForText(Duration waitTime) {
            return waitForEvent(textEvent, waitTime);
        }

        private boolean waitForEvent(CountDownLatch event, Duration waitTime) {
            try {
                return event.await(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                return false;
            }
        }

        boolean handshakeHappened() {
            return handshakeEvent.getCount() == 0;
        }

        boolean openHappened() {
            return openEvent.getCount() == 0;
        }

        boolean closeHappened() {
            return closeEvent.getCount() == 0;
        }

        boolean errorHappened() {
            return errorEvent.getCount() == 0;
        }

        boolean textHappened() {
            return textEvent.getCount() == 0;
        }

        boolean binaryHappened() {
            return binaryEvent.getCount() == 0;
        }
    }
}
