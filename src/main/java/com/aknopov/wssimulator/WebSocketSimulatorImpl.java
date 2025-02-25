package com.aknopov.wssimulator;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

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

    private Scenario scenario = new ScenarioImpl();

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

    private void startServer(SessionConfig config) {
        try {
            wsServer.start();
            wsServer.waitForStart(config.idleTimeout());
            history.addEvent(new Event(Instant.now(), EventType.STARTED, true));
        }
        catch (Exception e) {
            history.addEvent(new Event(Instant.now(), EventType.STARTED, false));
            history.addEvent(new Event(Instant.now(), EventType.ERROR, true)); //UC where is the text?
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

        history.addEvent(new Event(Instant.now(), EventType.STOPPED, scenario.isDone()));
    }

    @Override
    public void restart(Duration coolDownPeriodMs) {

    }

    private void playScenario() {
        scenario.play(act -> {
            try {
                Thread.sleep(act.delay()
                        .toMillis());
            }
            catch (InterruptedException e) {
                //Intentionally empty
            }

            //UC
            switch (act.eventType()) {
                case UPGRADE -> {
                }
                case OPEN -> {
                }
                case CLOSE -> {
                }
                case SERVER_MESSAGE -> {
                }
                case CLIENT_MESSAGE -> {
                }
                case WAIT -> {
                }
                case ACTION -> {
                }
                case EXPECT -> {
                }
                default -> {
                    // nothing
                }
            }
        });
    }

    //
    // EventListener implementation
    //

    @Override
    public void onHandshake(ProtocolUpgrade handshake) {

    }

    @Override
    public void onOpen(Map<String, Object> context) {

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
