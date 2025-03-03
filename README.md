# WebSocket simulator library

Java library to simulate WebSocket agents (client or server) primarily for creating unit tests. The idea is borrowed from the [WireMock](https://wiremock.org/) library.

The library is based on the [Tyrus](https://github.com/eclipse-ee4j/tyrus) implementation of the JSR WebSocket protocol.

## Overview

Two major classes are `WebSocketServerSimulator` and `WebSocketClientSimulator`. The first is supposed to be used for
WebSocket client tests. The other is for WebSocket server tests. The library contains unit tests that combine both 
simulators to create sensible communication. See [SimulatorsIntegrationTest.java](src%2Ftest%2Fjava%2Fcom%2Faknopov%2Fwssimulator%2FSimulatorsIntegrationTest.java).

## Example of server simulator

```java
int IDLE_SECS = 60;
int BUFFER_SIZE = 1024;
Duration ACTION_WAIT = Duration.ofSeconds(1);
SessionConfig config = new SessionConfig(A_PATH, Duration.ofSeconds(IDLE_SECS), BUFFER_SIZE);
String SERVER_RESPONSE_1 = "Response";

@Test
void testClientBehavior() throws Exception {

  WebSocketServerSimulator serverSimulator = new WebSocketServerSimulator(config, WebSocketServerSimulator.DYNAMIC_PORT);
  serverSimulator.getScenario()
          .expectProtocolUpgrade(this::validateUpgrade, ACTION_WAIT)
          .expectConnectionOpened(ACTION_WAIT)
          .expectMessage(this::validateTextMessage, ACTION_WAIT)
          .wait(Duration.ZERO)
          .sendMessage(SERVER_RESPONSE_1, Duration.ZERO)
          .expectConnectionClosed(this::validateNormalClose, ACTION_WAIT)
          .perform(() -> System.out.println("** All is done **"), Duration.ZERO);
  serverSimulator.start();

  // Do your part with the client...

  serverSimulator.getScenario().awaitCompletion(Duration.ofDays(1));
  serverSimulator.stop();

  assertFalse(serverSimulator.hasErrors());
}

private void validateTextMessage(WebSocketMessage message) throws ValidationException {
  if (message.getMessageType() != WebSocketMessage.MessageType.TEXT) {
    throw new ValidationException("Expected text message");
  }
}

private void validateUpgrade(ProtocolUpgrade upgrade) throws ValidationException {
  // Implement validation logic
}

private void validateNormalClose(WebSocketMessage message) throws ValidationException {
  // Implement validation logic
}
```

## Example of client simulator

```java
@Test
void testServerBehavior() throws Exception {
    WebSocketClientSimulator clientSimulator = new WebSocketClientSimulator("ws://localhost:" + SOME_PORT + A_PATH);
    clientSimulator.getScenario()
            .expectConnectionOpened(ACTION_WAIT)
            .sendMessage(MESSAGE_1, ACTION_WAIT.dividedBy(20))
            .expectMessage(this::validateTextMessage, ACTION_WAIT)
            .closeConnection(CloseCodes.NORMAL_CLOSURE, Duration.ZERO);
    clientSimulator.start();

    clientSimulator.getScenario().awaitCompletion(Duration.ofDays(1));

    assertFalse(clientSimulator.hasErrors());
}
```
## Noteworthy points
- `SessionConfig` - server configuration class containing context path, idle timeout and buffer size.
- `WebSocketMessage` - base class of text and binary messages.
- `ProtocolUpgrade` - available in server protocol validators. Contains connection request URL, query parameters, headers
  as well as the response status (such as HTTP-101). 
- `ValidationException` - exception thrown when validation fails.
- `Event` - result of execution scenario act or validation. Contains event type (including `ERROR`) and optional description.
- Simulator records all scenario acts, validation and timeout failures.
  Full list of events is available with `WebSocketSimulator::getHistory()` call.
- Scenario failures are created by validators or timeouts. List of errors can be requested with
  `WebSocketSimulator::getErrors` call. Simple check for errors can be done with `WebSocketSimulator::hasErrors`.
- Both client and server scenarios are run in a separate thread called either "ClientSimulator" or "ServerSimulator"
- `Scenario::perform` method can be used to create control points in scenario timeline. For example -
```java
CountDownLatch controlPoint = new CountDownLatch(1);

simulator.getScenario()
    ...
    .perform(controlPoint::countDown, Duration.ZERO)
    ...
simulator.start();

controlPoint.await();
// Proceed...
```