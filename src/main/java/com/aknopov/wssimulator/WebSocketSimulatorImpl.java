package com.aknopov.wssimulator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;

import javax.annotation.Nullable;

import com.aknopov.wssimulator.injection.ServiceLocator;
import com.aknopov.wssimulator.scenario.Event;
import com.aknopov.wssimulator.scenario.EventType;
import com.aknopov.wssimulator.scenario.History;
import com.aknopov.wssimulator.scenario.Scenario;
import com.aknopov.wssimulator.scenario.ScenarioImpl;
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
            try {
                Thread.sleep(act.delay() //UC delay before or wait
                        .toMillis());
            }
            catch (InterruptedException e) {
                //Intentionally empty
            }

            //UC
            switch (act.eventType()) {
                case UPGRADE -> {
                    // wait for `act.delay()` for handshake to happen
                    // Get `ProtocolUpgrade protoUpgrade` instance
                    //+record event
                    history.addEvent(Event.create(EventType.UPGRADE, true));
                    // act.consumer().accept(protoUpgrade);
                    // record error on exception
                }
                case OPEN -> {
                    // wait for `act.delay()` for connection to open
                    //+record event
                    history.addEvent(Event.create(EventType.OPEN, true));
                    // on exception - record error
                }
                case CLIENT_CLOSE -> {
                    // wait for `act.delay()` for connection to close
                    //+record event
                    history.addEvent(Event.create(EventType.CLIENT_CLOSE, true));
                    // record error on exception
                }
                case SERVER_MESSAGE -> {
                    // send message
                    act.consumer().accept(null);
                    //+record event
                    history.addEvent(Event.create(EventType.SERVER_MESSAGE, true));
                    // record error on exception
                }
                case CLIENT_MESSAGE -> {
                    // wait for `act.delay()` for a message
                    // get message
                    //UC record event with content
                    history.addEvent(Event.create(EventType.CLIENT_MESSAGE, true));
                    // record error on exception
                }
                case WAIT -> {
                    // wait for `act.delay()`
                    act.consumer().accept(null);
                    //+record event
                    history.addEvent(Event.create(EventType.WAIT, true));
                    // record error on exception
                }
                case ACTION -> {
                    // wait for `act.delay()`
                    act.consumer().accept(null);
                    //+record event
                    history.addEvent(Event.create(EventType.ACTION, true));
                    // record error on exception
                }
                case IO_ERROR -> {
                    // wait for `act.delay()` for the error
                    // get error
                    // validate: act.consumer().accept(error);
                    history.addEvent(Event.create(EventType.IO_ERROR, true));
                    // record error on exception
                }
                default -> {
                    // nothing
                }
            }
        });
    }

    //UC
    private void wait(Duration waitDuration) throws InterruptedException {
        Thread.sleep(waitDuration.toMillis());
    }

    //
    // EventListener implementation
    //

    @Override
    public void onHandshake(ProtocolUpgrade handshake) {

    }

    @Override
    public void onOpen(SimulatorEndpoint endpoint, Map<String, Object> context) {
        this.endpoint = endpoint;
    }

    @Override
    public void onClose(CloseReason closeReason) {

    }

    @Override
    public void onError(Throwable error) {

    }

    @Override
    public void onTextMessage(String message) {

    }

    @Override
    public void onBinaryMessage(ByteBuffer message) {

    }
}
