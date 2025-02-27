package com.aknopov.wssimulator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

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

/**
 * Implementation of WebSocketSimulator
 */
public class WebSocketSimulatorImpl implements WebSocketSimulator, EventListener {
    private final History history = new History();
    private final WebSocketServer wsServer;
    @Nullable
    private SimulatorEndpoint endpoint;
    private final Map<EventType, ResettableLock<?>> eventLocks = Map.of(
            EventType.UPGRADE, new ResettableLock<ProtocolUpgrade>(),
            EventType.OPEN, new ResettableLock<SimulatorEndpoint>(),
            EventType.CLIENT_CLOSE, new ResettableLock<CloseReason>(),
            EventType.CLIENT_MESSAGE, new ResettableLock<WebSocketMessage>(),
            EventType.IO_ERROR, new ResettableLock<Throwable>());

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
            history.addEvent(Event.create(EventType.STARTED, true));
        }
        catch (Exception e) {
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

        history.addEvent(Event.create(EventType.STOPPED, scenario.isDone()));
    }

    @Override
    public void sendTextMessage(String message) {
        try {
            Utils.requireNonNull(endpoint)
                    .sendTextMessage(message);
            history.addEvent(Event.create(EventType.SERVER_MESSAGE, true, "Text message"));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send text message before establishing connection"));
        }
        catch (IOException e) {
            history.addEvent(Event.error("Can't send text: " + e.getMessage()));
        }
    }

    @Override
    public void sendBinaryMessage(ByteBuffer message) {
        try {
            Utils.requireNonNull(endpoint)
                    .sendBinaryMessage(message);
            history.addEvent(Event.create(EventType.SERVER_MESSAGE, true, "Binary message"));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send binary message before establishing connection"));
        }
        catch (IOException e) {
            history.addEvent(Event.error("Can't send binary: " + e.getMessage()));
        }
    }

    private void playScenario() {
        scenario.play(act -> {
            switch (act.eventType()) {
                case UPGRADE -> process(() -> {
                    ProtocolUpgrade protoUpgrade = waitFor(EventType.UPGRADE, act.delay(), ProtocolUpgrade.class);
                    history.addEvent(Event.create(EventType.UPGRADE, true));
                    consumeData(act.consumer(), protoUpgrade);
                });
                case OPEN -> process(() -> {
                    SimulatorEndpoint endpoint = waitFor(EventType.OPEN, act.delay(), SimulatorEndpoint.class);
                    this.endpoint = endpoint;
                    history.addEvent(Event.create(EventType.OPEN, true));
                    consumeData(act.consumer(), endpoint); //UC
                });
                case CLIENT_CLOSE -> {
                    CloseReason reason = waitFor(EventType.CLIENT_CLOSE, act.delay(), CloseReason.class);
                    history.addEvent(Event.create(EventType.CLIENT_CLOSE, true));
                    consumeData(act.consumer(), reason);
                }
                case SERVER_MESSAGE -> process(() -> {
                    consumeData(act.consumer(), null);
                    history.addEvent(Event.create(EventType.SERVER_MESSAGE, true));
                });
                case CLIENT_MESSAGE -> process(() -> {
                    WebSocketMessage message = waitFor(EventType.CLIENT_MESSAGE, act.delay(), WebSocketMessage.class);
                    history.addEvent(Event.create(EventType.CLIENT_MESSAGE, true));
                    consumeData(act.consumer(), message);
                });
                case WAIT -> process(() -> {
                    wait(act.delay());
                    history.addEvent(Event.create(EventType.WAIT, true));
                });
                case ACTION -> process(() -> {
                    wait(act.delay());
                    history.addEvent(Event.create(EventType.ACTION, true));
                    consumeData(act.consumer(), null);
                });
                case IO_ERROR -> process(() -> {
                    Throwable error = waitFor(EventType.IO_ERROR, act.delay(), Throwable.class);
                    history.addEvent(Event.create(EventType.IO_ERROR, true));
                    consumeData(act.consumer(), error);
                });
                default -> {
                    history.addEvent(Event.error("Internal error, act " + act.eventType() + " is not processable"));
                }
            }
        });
    }

    private void process(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (NullPointerException ex) {
            history.addEvent(Event.error("NPE at " + Arrays.toString(ex.getStackTrace())));
        }
        catch (ScenarioException ex) {
            history.addEvent(Event.error("Expected action didn't happen at" + Arrays.toString(ex.getStackTrace())));
        }
        catch (ValidationException ex) {
            history.addEvent(Event.error("Expectation wasn't fulfilled: " + ex.getMessage()));
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
        try {
            return Utils.requireNonNull((ResettableLock<T>)eventLocks.get(eventType))
                    .await(waitDuration);
        }
        catch (InterruptedException e) {
            throw new ScenarioException("Wait interrupted");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void releaseEvent(EventType eventType, T payload) {
        try {
            Utils.requireNonNull((ResettableLock<T>)eventLocks.get(eventType))
                    .release(payload);
        }
        catch (InterruptedException e) {
            throw new ScenarioException("Release interrupted");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void consumeData(Consumer<?> consumer, @Nullable T data) {
        ((Consumer<T>)consumer).accept(data);
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
