package com.example.behaviour.Commands;

import com.example.behaviour.Events.NetworkEvent;
import com.example.interfaces.ManagerCommand;
import com.example.network.NetworkManager;
import com.example.network.Link;
import com.example.network.Packt;
import com.example.network.Pc;
import java.util.UUID;
import java.util.List;

public class SendThroughCircuit implements ManagerCommand 
{
    Pc origin;
    String Message;
    UUID token;

    public SendThroughCircuit(Pc origin, String Message, UUID token)
    {
        this.origin = origin;
        this.Message = Message;
        this.token = token;
    }

    public void execute(NetworkManager context)
    {
        List<Link> connection = context.getConn(token);

        Packt packt = new Packt(Message, connection);
        NetworkEvent event = new NetworkEvent(packt);

        origin.queueInstruction(event);
    }
}
