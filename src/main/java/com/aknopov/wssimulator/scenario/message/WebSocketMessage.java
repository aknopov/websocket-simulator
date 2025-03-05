package com.aknopov.wssimulator.scenario.message;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

/**
 * Communication message between simulator and application, either text or binary
 */
public abstract class WebSocketMessage
{
    public enum MessageType
    {
        TEXT,
        BINARY
    }

    private final MessageType messageType;

    WebSocketMessage(MessageType messageType)
    {
        this.messageType = messageType;
    }

    public MessageType getMessageType()
    {
        return messageType;
    }

    @Nullable
    public abstract String getMessageText();

    @Nullable
    public abstract ByteBuffer getMessageBytes();
}
