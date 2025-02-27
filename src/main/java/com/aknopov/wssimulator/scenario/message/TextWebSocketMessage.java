package com.aknopov.wssimulator.scenario.message;

import java.nio.ByteBuffer;
import java.util.Objects;

import javax.annotation.Nullable;

public class TextWebSocketMessage extends WebSocketMessage
{
    private final String text;

    public TextWebSocketMessage(String text)
    {
        super(MessageType.TEXT);
        this.text = text;
    }

    @Nullable
    @Override
    public String getMessageText()
    {
        return text;
    }

    @Nullable
    @Override
    public ByteBuffer getMessageBytes()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return "TextWebSocketMessage{" + "text='" + text + '\'' + '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TextWebSocketMessage that))
        {
            return false;
        }
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getMessageType(), text);
    }
}
