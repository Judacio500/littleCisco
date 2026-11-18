package com.example.network.devices;

public class Router extends NetworkNode
{
    public Router(String displayName)
    {
        super(displayName);
    }

    @Override
    public boolean isIpForwardingEnabled()
    {
        return true;
    }
}