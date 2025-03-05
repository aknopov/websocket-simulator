package com.aknopov.wssimulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.scenario.Act;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.ScenarioImpl;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.scenario.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.TextWebSocketMessage;
import com.aknopov.wssimulator.scenario.message.WebSocketMessage;
import jakarta.websocket.CloseReason;

import static com.aknopov.wssimulator.Utils.requireNonNull;

/**
 * Common functionality of client and server simulators
 */
public abstract class WebSocketSimulatorBase implements WebSocketSimulator, EventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSimulatorBase.class);

    protected final History history = new History();
    protected final Scenario scenario = new ScenarioImpl(this);
    protected final Thread scenarioThread;
    @Nullable
    protected SimulatorEndpoint endpoint;
    private final Map<EventType, ResettableLock<?>> eventLocks = Map.of(
            EventType.UPGRADE, new ResettableLock<ProtocolUpgrade>(),
            EventType.OPEN, new ResettableLock<SimulatorEndpoint>(),
            EventType.CLOSED, new ResettableLock<CloseReason.CloseCode>(),
            EventType.RECEIVE_MESSAGE, new ResettableLock<WebSocketMessage>(),
            EventType.IO_ERROR, new ResettableLock<Throwable>());

    protected WebSocketSimulatorBase(String threadName) {
        this.scenarioThread = new Thread(this::playScenario, threadName);
    }

    //
    // WebSocketSimulator implementation
    //

    @Override
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public List<Event> getHistory() {
        return history.getEvents();
    }

    @Override
    public void setEndpoint(SimulatorEndpoint endpoint) {
        this.endpoint = endpoint;
        logger.debug("Connection opened");
    }

    @Override
    public void sendMessage(WebSocketMessage message) {
        switch (message.getMessageType()) {
            case TEXT -> sendTextMessage(requireNonNull(message.getMessageText()));
            case BINARY -> sendBinaryMessage(requireNonNull(message.getMessageBytes()));
        }
    }

    private void sendTextMessage(String message) {
        logger.debug("Requested to send text '{}'", message);
        try {
            requireNonNull(endpoint).sendTextMessage(message);
            history.addEvent(Event.create(EventType.SEND_MESSAGE, message));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send text message before establishing connection"));
        }
        catch (UncheckedIOException e) {
            history.addEvent(Event.error("Can't send text: " + e.getMessage()));
        }
    }

    private void sendBinaryMessage(ByteBuffer message) {
        logger.debug("Requested send binary message with {} bytes", message.remaining());
        try {
            requireNonNull(endpoint).sendBinaryMessage(message);
            history.addEvent(Event.create(EventType.SEND_MESSAGE, "Binary, len=" + message.remaining()));
        }
        catch (IllegalStateException e) {
            history.addEvent(Event.error("Attempted to send binary message before establishing connection"));
        }
        catch (UncheckedIOException e) {
            history.addEvent(Event.error("Can't send binary: " + e.getMessage()));
        }
    }

    @Override
    public boolean hasErrors() {
        return history.getEvents().stream().anyMatch(e -> e.eventType() == EventType.ERROR);
    }

    @Override
    public List<Event> getErrors() {
        return history.getEvents()
                .stream()
                .filter(e -> e.eventType() == EventType.ERROR)
                .toList();
    }

    @Override
    public void start() {
        scenarioThread.start();
    }

    @Override
    @SuppressWarnings("Interruption")
    public void stop() {
        scenario.requestStop();
        if (scenarioThread.isAlive()) {
            scenarioThread.interrupt();
        }
        history.addEvent(Event.create(EventType.STOPPED));
    }

    @Override
    public boolean awaitScenarioCompletion(Duration waitDuration) {
        return scenario.awaitCompletion(waitDuration);
    }

    @Override
    public boolean isScenarioDone() {
        return scenario.isDone();
    }

    /**
     * Process scenario acts
     */
    private void playScenario() {
        try {
            scenario.play(act -> {
                switch (act.eventType()) {
                    case UPGRADE -> process(() -> {
                        ProtocolUpgrade protoUpgrade = waitFor(act, ProtocolUpgrade.class);
                        consumeData(act, protoUpgrade);
                    });
                    case OPEN -> process(() -> {
                        SimulatorEndpoint endpoint = waitFor(act, SimulatorEndpoint.class);
                        consumeData(act, endpoint); // this triggers `setEndpoint(endpoint)`
                    });
                    case CLOSED -> {
                        CloseReason.CloseCode code = waitFor(act, CloseReason.CloseCode.class);
                        consumeData(act, code);
                    }
                    case RECEIVE_MESSAGE -> process(() -> {
                        WebSocketMessage message = waitFor(act, WebSocketMessage.class);
                        consumeData(act, message);
                    });
                    case IO_ERROR -> process(() -> {
                        Throwable error = waitFor(act, Throwable.class);
                        consumeData(act, error);
                    });
                    case SEND_MESSAGE -> process(() -> {
                        WebSocketMessage message = provideData(act, WebSocketMessage.class);
                        sendMessage(message); // history is updated separately for text and binary
                    });
                    case DO_CLOSE -> process(() -> {
                        CloseReason.CloseCode code = provideData(act, CloseReason.CloseCode.class);
                        Utils.requireNonNull(endpoint)
                                .closeConnection(code);
                        history.addEvent(Event.create(EventType.DO_CLOSE));
                    });
                    case WAIT -> process(() -> {
                        wait(act.delay());
                        history.addEvent(Event.create(EventType.WAIT));
                    });
                    case ACTION -> process(() -> {
                        wait(act.delay());
                        history.addEvent(Event.create(EventType.ACTION));
                        consumeData(act, null);
                    });
                    default -> history.addEvent(Event.error("Internal error, act " + act.eventType() + " is not processable"));
                }
            });
        }
        catch (ScenarioInterruptedException ex) {
            history.addEvent(Event.error("Scenario run has been interrupted: " + ex.stringify()));
        }
    }

    // VisibleForTesting
    void process(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (NullPointerException ex) {
            history.addEvent(Event.error("NPE at " + Arrays.toString(ex.getStackTrace())));
        }
        catch (TimeoutException ex) {
            history.addEvent(Event.error(ex.getMessage() + ": " + ex.stringify()));
        }
        catch (ValidationException ex) {
            history.addEvent(Event.error("Expectation wasn't fulfilled: " + ex.getMessage()));
        }
        catch (UncheckedIOException ex) {
            history.addEvent(Event.error("IO exception thrown: " + ex.getCause()));
        }
    }

    private static void wait(Duration waitDuration) {
        logger.debug("Waiting for {} msec", waitDuration.toMillis());
        try {
            Thread.sleep(waitDuration.toMillis());
        }
        catch (InterruptedException e) {
            throw new ScenarioInterruptedException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T waitFor(Act<?> act, Class<T> klaz) {
        logger.debug("Waiting {} for {} msec", klaz.getSimpleName(), act.delay().toMillis());
        ResettableLock<T> lock = (ResettableLock<T>)requireNonNull(eventLocks.get(act.eventType()));
        try {
            T ret = requireNonNull(lock.await(act.delay()));
            history.addEvent(Event.create(act.eventType()));
            return ret;
        }
        catch (InterruptedException e) {
            throw new ScenarioInterruptedException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void releaseEvent(EventType eventType, T payload) {
        logger.debug("Releasing {}", payload.getClass().getSimpleName());
        requireNonNull((ResettableLock<T>)eventLocks.get(eventType))
                .release(payload);
    }

    @SuppressWarnings("unchecked")
    private static <T> void consumeData(Act<?> act, @Nullable T data) {
        requireNonNull((Consumer<T>)act.consumer()).accept(data);
    }

    @SuppressWarnings("unchecked")
    private static <T> T provideData(Act<?> act, Class<T> unused) {
        return requireNonNull((Supplier<T>)act.supplier()).get();
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
    public void onClose(CloseReason.CloseCode closeCode) {
        releaseEvent(EventType.CLOSED, closeCode);
    }

    @Override
    public void onError(Throwable error) {
        releaseEvent(EventType.IO_ERROR, error);
    }

    @Override
    public void onTextMessage(String message) {
        releaseEvent(EventType.RECEIVE_MESSAGE, new TextWebSocketMessage(message));
    }

    @Override
    public void onBinaryMessage(ByteBuffer message) {
        releaseEvent(EventType.RECEIVE_MESSAGE, new BinaryWebSocketMessage(message));
    }
}
