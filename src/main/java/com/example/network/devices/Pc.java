package com.example.network.devices;

import java.util.UUID;
import com.example.behaviour.BasicConsole;

public class Pc extends NetworkNode
{
    public BasicConsole console = new BasicConsole();
    private UUID connToken;

    public Pc(String displayName)
    {
        super(displayName);
    }

    public UUID getConnToken()
    {
        return connToken;
    }

    public void setConnToken(UUID connToken)
    {
        this.connToken = connToken;
    }

    @Override
    public boolean isIpForwardingEnabled()
    {
        return false;
    }
}