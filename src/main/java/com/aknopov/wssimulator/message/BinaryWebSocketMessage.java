package com.aknopov.wssimulator.message;

import java.nio.ByteBuffer;
import java.util.Objects;

import javax.annotation.Nullable;


public class BinaryWebSocketMessage extends WebSocketMessage
{
    private final ByteBuffer data;

    public BinaryWebSocketMessage(ByteBuffer data)
    {
        super(MessageType.BINARY);
        this.data = data;
    }

    @Nullable
    @Override
    public String getMessageText()
    {
        return null;
    }

    @Nullable
    @Override
    public ByteBuffer getMessageBytes()
    {
        return data;
    }

    @Override
    public String toString()
    {
        return "BinaryWebSocketMessage{" + "data size=" + data.capacity() + " bytes}";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof BinaryWebSocketMessage that))
        {
            return false;
        }
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getMessageType(), data);
    }
}
