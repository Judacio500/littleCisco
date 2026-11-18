package com.example.network.packets;

import java.nio.charset.StandardCharsets;

public class Payload
{
    private final byte[] rawData;

    public Payload(byte[] rawData)
    {
        this.rawData = rawData;
    }

    public Payload(String text)
    {
        this.rawData = text.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getRawData()
    {
        return rawData;
    }

    public String asString()
    {
        return new String(rawData, StandardCharsets.UTF_8);
    }
}