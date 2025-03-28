package com.aknopov.wssimulator.simulator;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.ProtocolUpgrade;
import com.aknopov.wssimulator.ResettableLock;
import com.aknopov.wssimulator.SimulatorEndpoint;
import com.aknopov.wssimulator.Utils;
import com.aknopov.wssimulator.WebSocketSimulator;
import com.aknopov.wssimulator.scenario.Act;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.Scenario;
import com.aknopov.wssimulator.scenario.ScenarioImpl;
import com.aknopov.wssimulator.ScenarioInterruptedException;
import com.aknopov.wssimulator.TimeoutException;
import com.aknopov.wssimulator.scenario.ValidationException;
import com.aknopov.wssimulator.message.BinaryWebSocketMessage;
import com.aknopov.wssimulator.message.TextWebSocketMessage;
import com.aknopov.wssimulator.message.WebSocketMessage;
import com.aknopov.wssimulator.tyrus.WebSocketEndpoint;
import jakarta.websocket.CloseReason.CloseCodes;

import static com.aknopov.wssimulator.Utils.requireNonNull;
import static java.util.function.Predicate.not;

/**
 * Common functionality of client and server simulators
 */
public abstract class WebSocketSimulatorBase implements WebSocketSimulator, EventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSimulatorBase.class);

    private static final Set<EventType> IGNORED_EVENTS = Set.of(EventType.STARTED, EventType.ERROR);
    private static final Set<EventType> MATCHING_EVENTS = Arrays.stream(EventType.values()).filter(not(IGNORED_EVENTS::contains))
            .collect(Collectors.toSet());

    protected final History history = new History();
    protected final Scenario scenario = new ScenarioImpl();
    protected final Thread scenarioThread;
    @Nullable
    protected SimulatorEndpoint endpoint;
    private final Map<EventType, ResettableLock<?>> eventLocks = Map.of(
            EventType.UPGRADE, new ResettableLock<>(ProtocolUpgrade.class),
            EventType.OPEN, new ResettableLock<>(SimulatorEndpoint.class),
            EventType.CLOSED, new ResettableLock<>(CloseCodes.class),
            EventType.RECEIVE_MESSAGE, new ResettableLock<>(WebSocketMessage.class));
    private final List<EventType> expectedEvents = new ArrayList<>();

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

    // VisibleForTesting
    void setEndpoint(SimulatorEndpoint endpoint) {
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
        try {
            requireNonNull(endpoint).sendTextMessage(message);
            history.addEvent(Event.create(EventType.SEND_MESSAGE, message));
        }
        catch (IllegalStateException e) {
            recordError("Attempted to send text message before establishing connection");
        }
        catch (UncheckedIOException e) {
            recordError("Can't send text: " + e.getMessage());
        }
    }

    private void sendBinaryMessage(ByteBuffer message) {
        try {
            requireNonNull(endpoint).sendBinaryMessage(message);
            history.addEvent(Event.create(EventType.SEND_MESSAGE, "Binary, len=" + message.remaining()));
        }
        catch (IllegalStateException e) {
            recordError("Attempted to send binary message before establishing connection");
        }
        catch (UncheckedIOException e) {
            recordError("Can't send binary: " + e.getMessage());
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
    public boolean noMoreEvents() {
        if (!scenario.isDone()) {
            List<EventType> remainedActs = StreamSupport.stream(scenario.spliterator(), false)
                    .map(Act::eventType)
                    .toList();
            recordError("Scenario hasn't been completed. Remained acts: " + remainedActs);
            return false;
        }

        List<EventType> expectedFiltered = expectedEvents.stream().filter(MATCHING_EVENTS::contains).toList();
        List<EventType> actualFiltered = getHistory().stream().map(Event::eventType).filter(MATCHING_EVENTS::contains).toList();
        if (!expectedFiltered.equals(actualFiltered)) {
            recordError(String.format("Scenario doesn't match actual events.%nExpected: %s%nActual: %s",
                    expectedFiltered, getHistory().stream().filter(e -> MATCHING_EVENTS.contains(e.eventType())).toList()));
            return false;
        }
        return true;
    }

    protected void recordError(String message) {
        history.addEvent(Event.error(message));
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
    public boolean awaitScenarioStart(Duration waitDuration) {
        return scenario.awaitStart(waitDuration);
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
            for (Act<?> act: scenario) {
                expectedEvents.add(act.eventType());
                playOneAct(act);
            }
        }
        catch (ScenarioInterruptedException ex) {
            recordError("Scenario run has been interrupted: " + Utils.stringify(ex));
        }
        finally {
            scenario.markCompletion();
        }
    }

    private void playOneAct(Act<?> act) {
        switch (act.eventType()) {
            case UPGRADE -> process(() -> {
                ProtocolUpgrade protoUpgrade = waitFor(act, ProtocolUpgrade.class);
                consumeData(act, protoUpgrade);
            });
            case OPEN -> process(() -> {
                waitFor(act, WebSocketEndpoint.class); // endpoint is already set
            });
            case CLOSED -> process(() -> {
                CloseCodes code = waitFor(act, CloseCodes.class);
                consumeData(act, code);
            });
            case RECEIVE_MESSAGE -> process(() -> {
                WebSocketMessage message = waitFor(act, WebSocketMessage.class);
                consumeData(act, message);
            });
            case SEND_MESSAGE -> process(() -> {
                wait(act.delay());
                WebSocketMessage message = provideData(act, WebSocketMessage.class);
                sendMessage(message); // history is updated separately for text and binary
            });
            case DO_CLOSE -> process(() -> {
                wait(act.delay());
                CloseCodes code = provideData(act, CloseCodes.class);
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
                consumeData(act, null);
                history.addEvent(Event.create(EventType.ACTION));
            });
            default -> recordError("Internal error, act " + act.eventType() + " is not processable");
        }
    }

    // VisibleForTesting
    void process(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (NullPointerException ex) {
            recordError("NPE at " + Arrays.toString(ex.getStackTrace()));
        }
        catch (TimeoutException ex) {
            recordError(ex.getMessage() + ": " + Utils.stringify(ex));
        }
        catch (ValidationException ex) {
            recordError("Expectation wasn't fulfilled: " + ex.getMessage());
        }
        catch (UncheckedIOException ex) {
            recordError("IO exception thrown: " + ex.getCause());
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
        logger.debug("Waiting {} object for {} msec", klaz.getSimpleName(), act.delay().toMillis());
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
        logger.debug("Releasing {} object", payload.getClass().getSimpleName());
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
        this.endpoint = endpoint;
        releaseEvent(EventType.OPEN, endpoint);
    }

    @Override
    public void onClose(CloseCodes closeCode) {
        releaseEvent(EventType.CLOSED, closeCode);
    }

    @Override
    public void onError(Throwable error) {
        // Nothing
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
