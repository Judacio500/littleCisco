package com.example.behaviour.Commands;

import com.example.interfaces.ManagerCommand;
import com.example.network.NetworkManager;
import com.example.network.Pc;
import com.example.network.Link;
import com.example.behaviour.Events.ConnectionEvent;
import java.util.List;
import java.util.UUID;


public class CircuitSwitching implements ManagerCommand
{
    Pc origin, destination;
    UUID token;

    public CircuitSwitching(Pc origin, Pc destination)
    {
        this.origin = origin;
        this.destination = destination;
    }

    public void execute(NetworkManager context)
    {
        List<Link> connection = context.network.getRouting(origin, destination);

        for(Link l : connection)
        {
            l.setBusy();
        }
        token = context.addConnection(connection);

        ConnectionEvent event = new ConnectionEvent(token);
        origin.queueInstruction(event);
    }
}
