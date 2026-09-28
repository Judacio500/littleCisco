package com.example.behaviour.Commands;

import java.util.List;
import java.util.UUID;

import com.example.behaviour.Events.ConnectionEvent;
import com.example.interfaces.ManagerCommand;
import com.example.network.Link;
import com.example.network.NetworkManager;
import com.example.network.Pc;

public class TeardownConn implements ManagerCommand
{
    UUID token;
    Pc origin;

    public TeardownConn(Pc origin, UUID token)
    {
        this.token = token;
        this.origin = origin;
    }

    public void execute(NetworkManager context)
    {
        List<Link> connections = context.getConn(token);

        for(Link l : connections)
        {
            l.setAvailable();
        }

        context.eraseConn(token);

        ConnectionEvent event = new ConnectionEvent(null);
        origin.queueInstruction(event);
    }    
}
