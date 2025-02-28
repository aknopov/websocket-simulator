package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.aknopov.wssimulator.injection.ServiceLocator;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.ScenarioException;
import com.aknopov.wssimulator.scenario.ScenarioImpl;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import com.aknopov.wssimulator.tyrus.WebSocketServer;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCode;

/**
 * Implementation of WebSocketSimulator
 */
public class WebSocketSimulatorImpl implements WebSocketSimulator, EventListener {
    private final History history = new History();
    private final Map<EventType, ResettableLock<?>> eventLocks = Map.of(
            EventType.UPGRADE, new ResettableLock<ProtocolUpgrade>(),
            EventType.OPEN, new ResettableLock<SimulatorEndpoint>(),
            EventType.CLIENT_CLOSE, new ResettableLock<CloseReason>(),
            EventType.CLIENT_MESSAGE, new ResettableLock<WebSocketMessage>(),
            EventType.IO_ERROR, new ResettableLock<Throwable>());

    @Nullable
    private SimulatorEndpoint endpoint;
    private final WebSocketServer wsServer;

    private Scenario scenario = new ScenarioImpl(this);

    /**
     * Creates simulator with given configuration
     *
     * @param port port number, 0 - is for dynamic
     * @param config session configuration
     */
    public WebSocketSimulatorImpl(int port, SessionConfig config) {

        ServiceLocator.init(config, this);
        this.wsServer = port != 0
                ? new WebSocketServer("localhost", "/", Map.of(), port)
                : new WebSocketServer("localhost", "/", Map.of());
        startServer(config);
    }

    @SuppressWarnings("NullAway")
    private void startServer(SessionConfig config) {
        try {
            wsServer.start();
            wsServer.waitForStart(config.idleTimeout());
            history.addEvent(Event.create(EventType.STARTED));
        }
        catch (RuntimeException e) {
            history.addEvent(Event.error(e.getMessage()));
        }
    }

    //
    // WebSocketSimulator implementation
    //

    @Override
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public History getHistory() {
        return history;
    }

    @Override
    public void resetHistory() {
        history.reset();
    }

    @Override
    public int getServerPort() {
        return wsServer.getPort();
    }

    @Override
    public void start() {
        playScenario();
    }

    @Override
    public void stop() {
        wsServer.stop();

        history.addEvent(Event.create(EventType.STOPPED));
    }

    @Override
    public void sendTextMessage(String message) {
        try {
            Utils.requireNonNull(endpoint)
                    .sendTextMessage(message);
            history.addEvent(Event.create(EventType.SERVER_MESSAGE, "Text message"));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send text message before establishing connection"));
        }
        catch (UncheckedIOException e) {
            history.addEvent(Event.error("Can't send text: " + e.getMessage()));
        }
    }

    @Override
    public void sendBinaryMessage(ByteBuffer message) {
        try {
            Utils.requireNonNull(endpoint)
                    .sendBinaryMessage(message);
            history.addEvent(Event.create(EventType.SERVER_MESSAGE, "Binary message"));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send binary message before establishing connection"));
        }
        catch (UncheckedIOException e) {
            history.addEvent(Event.error("Can't send binary: " + e.getMessage()));
        }
    }

    @Override
    public void setEndpoint(SimulatorEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private void playScenario() {
        scenario.play(act -> {
            switch (act.eventType()) {
                case UPGRADE -> process(() -> {
                    ProtocolUpgrade protoUpgrade = waitFor(EventType.UPGRADE, act.delay(), ProtocolUpgrade.class);
                    history.addEvent(Event.create(EventType.UPGRADE));
                    consumeData(act.consumer(), protoUpgrade);
                });
                case OPEN -> process(() -> {
                    SimulatorEndpoint endpoint = waitFor(EventType.OPEN, act.delay(), SimulatorEndpoint.class);
                    history.addEvent(Event.create(EventType.OPEN));
                    consumeData(act.consumer(), endpoint);
                });
                case CLIENT_CLOSE -> {
                    CloseReason reason = waitFor(EventType.CLIENT_CLOSE, act.delay(), CloseReason.class);
                    history.addEvent(Event.create(EventType.CLIENT_CLOSE, "Close code " + reason.getCloseCode()));
                    consumeData(act.consumer(), reason);
                }
                case CLIENT_MESSAGE -> process(() -> {
                    WebSocketMessage message = waitFor(EventType.CLIENT_MESSAGE, act.delay(), WebSocketMessage.class);
                    history.addEvent(Event.create(EventType.CLIENT_MESSAGE));
                    consumeData(act.consumer(), message);
                });
                case IO_ERROR -> process(() -> {
                    Throwable error = waitFor(EventType.IO_ERROR, act.delay(), Throwable.class);
                    history.addEvent(Event.create(EventType.IO_ERROR));
                    consumeData(act.consumer(), error);
                });
                case SERVER_MESSAGE -> process(() -> {
                    WebSocketMessage message = provideData(act.supplier(), WebSocketMessage.class);
                    consumeData(act.consumer(), message);
                    history.addEvent(Event.create(EventType.SERVER_MESSAGE));
                });
                case SERVER_CLOSE -> process(() -> {
                    CloseCode code = provideData(act.supplier(), CloseCode.class);
                    Utils.requireNonNull(endpoint).closeConnection(code);
                    history.addEvent(Event.create(EventType.SERVER_CLOSE));
                });
                case WAIT -> process(() -> {
                    wait(act.delay());
                    history.addEvent(Event.create(EventType.WAIT));
                });
                case ACTION -> process(() -> {
                    wait(act.delay());
                    history.addEvent(Event.create(EventType.ACTION));
                    consumeData(act.consumer(), null);
                });
                default ->
                    history.addEvent(Event.error("Internal error, act " + act.eventType() + " is not processable"));
            }
        });
    }

    private void process(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (NullPointerException ex) { // probably waiting for an object timed out
            history.addEvent(Event.error("NPE at " + Arrays.toString(ex.getStackTrace())));
        }
        catch (ScenarioException ex) {
            history.addEvent(Event.error("Expected action didn't happen at" + Arrays.toString(ex.getStackTrace())));
        }
        catch (ValidationException ex) {
            history.addEvent(Event.error("Expectation wasn't fulfilled: " + ex.getMessage()));
        }
        catch (UncheckedIOException ex) {
            history.addEvent(Event.error("IO exception thrown: " + ex.getCause()));
        }
    }

    private static void wait(Duration waitDuration) {
        try {
            Thread.sleep(waitDuration.toMillis());
        }
        catch (InterruptedException e) {
            throw new ScenarioException("Wait interrupted");
        }
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    private <T> T waitFor(EventType eventType, Duration waitDuration, Class<T> unused) {
        ResettableLock<T> lock = (ResettableLock<T>)Utils.requireNonNull(eventLocks.get(eventType));
        try {
            return Utils.requireNonNull(lock.await(waitDuration));
        }
        catch (InterruptedException e) {
            throw new ScenarioException("Wait interrupted");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void releaseEvent(EventType eventType, T payload) {
        Utils.requireNonNull((ResettableLock<T>)eventLocks.get(eventType))
                .release(payload);
    }

    @SuppressWarnings("unchecked")
    private static <T> void consumeData(@Nullable Consumer<?> consumer, @Nullable T data) {
        Utils.requireNonNull((Consumer<T>)consumer).accept(data);
    }

    @SuppressWarnings("unchecked")
    private static <T> T provideData(@Nullable Supplier<?> supplier, Class<T> unused) {
        return Utils.requireNonNull((Supplier<T>)supplier).get();
    }

    //
    // EventListener implementation
    //

    @Override
    public void onHandshake(ProtocolUpgrade handshake) {
        releaseEvent(EventType.UPGRADE, handshake);
    }

    @Override
    public void onOpen(SimulatorEndpoint endpoint, Map<String, Object> context) {
        releaseEvent(EventType.OPEN, endpoint);
    }

    @Override
    public void onClose(CloseReason closeReason) {
        releaseEvent(EventType.CLIENT_CLOSE, closeReason);
    }

    @Override
    public void onError(Throwable error) {
        releaseEvent(EventType.IO_ERROR, error);
    }

    @Override
    public void onTextMessage(String message) {
        releaseEvent(EventType.CLIENT_MESSAGE, new TextWebSocketMessage(message));
    }

    @Override
    public void onBinaryMessage(ByteBuffer message) {
        releaseEvent(EventType.CLIENT_MESSAGE, new BinaryWebSocketMessage(message));
    }
}
