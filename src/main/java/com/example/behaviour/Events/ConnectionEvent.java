package com.example.behaviour.Events;

import com.example.interfaces.Event;
import com.example.network.Pc;

import java.util.UUID;

public class ConnectionEvent implements Event
{
    UUID token;
    public ConnectionEvent(UUID token)
    {
        this.token = token;
    }   

    public void execute(Pc context)
    {
        context.setConnToken(token);
    }
}
