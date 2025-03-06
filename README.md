[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
![GitHub Actions Workflow Status](https://github.com/aknopov/websocket-simulator/actions/workflows/gradle.yml/badge.svg?branch=main)
![coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/aknopov/websocket-simulator/main/.github/badges/jacoco.json)

# WebSocket simulator library

Java library to simulate WebSocket agents (clients or servers) primarily for creating unit tests. The idea is borrowed from the [WireMock](https://wiremock.org/) library.

The library is based on the [Tyrus](https://github.com/eclipse-ee4j/tyrus) implementation of the WebSocket JSR.

## Overview

Two major classes are `WebSocketServerSimulator` and `WebSocketClientSimulator`. The first is supposed to be used for
WebSocket client tests. The other - for server tests. The library contains unit tests that combine both 
simulators to create reasonable communication example. See [SimulatorsIntegrationTest.java](src%2Ftest%2Fjava%2Fcom%2Faknopov%2Fwssimulator%2FSimulatorsIntegrationTest.java).

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

  serverSimulator.awaitScenarioCompletion(Duration.ofDays(1));
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

    clientSimulator.awaitScenarioCompletion(Duration.ofDays(1));

    assertFalse(clientSimulator.hasErrors());
}
```
## Notes
- <ins>Server simulator does not allow multiple connections.</ins> Scenario is "played" sequentially in a dedicated thread
  called "ClientSimulator" or "ServerSimulator". 
- `SessionConfig` - server configuration class containing context path, idle timeout and buffer size.
- `WebSocketMessage` - base class of text and binary messages.
- `ProtocolUpgrade` - available only in server protocol validators. Contains connection request URL, query parameters, headers
  along with response status code (such as HTTP-101). 
- `ValidationException` - exception thrown when validation fails.
- `Event` - result of execution scenario act or validation. Contains event type (including `ERROR`) and optional description.
- Simulator records all scenario acts, validation and timeout failures.
  Full list of events is available with `WebSocketSimulator::getHistory()` call.
- List of errors can be filtered from other events with call of `WebSocketSimulator::getErrors`.
  Simple check for errors can be done with `WebSocketSimulator::hasErrors`.
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