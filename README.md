# WebSocket simulator library

Java library to simulate websocket (client or server) behavior primarily for creating unit tests.
The idea is borrowed from [WireMock](https://wiremock.org/) library. 

## Overview

Two major classes are supposed to be created - client and server simulators.

Client simulator is supposed to connect to local remote server and perform some operations - send and receive message with validation,
close connection described as a sequence of actions in a "scenario".

Server simulator is supposed to start on the local host using predefined or dynamic port.
Server simulator behavior is also supposed to be described in a "scenario" that also should include web socket upgrade validation
as a source of authentication.

Library is supposed to use [Tyrus](https://github.com/eclipse-ee4j/tyrus) implementation of web socket protocol.