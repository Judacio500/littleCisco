package com.example.behaviour.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.behaviour.Events.NetworkEvent;
import com.example.interfaces.ManagerCommand;
import com.example.network.Link;
import com.example.network.NetworkManager;
import com.example.network.Packt;
import com.example.network.Pc;

public class PacketSwitching implements ManagerCommand
{
    Pc origin, destination;
    String message;

    public PacketSwitching(Pc origin, Pc destination, String message) 
    {
        this.origin = origin;
        this.destination = destination;
        this.message = message;
    }

    public void execute(NetworkManager context)
    {
        List<Link> connection = context.network.getRouting(origin, destination);

        int packtSize = 5;

        String[] messageParts = message.split("(?<=\\G.{" + packtSize + "})");
   
        UUID id = UUID.randomUUID();

        for(int i=0; i<messageParts.length; i++)
        {
            String m = messageParts[i];
            Packt packt = new Packt(m, id, messageParts.length, i+1, connection); 
        
            NetworkEvent event = new NetworkEvent(packt);

            origin.queueInstruction(event);
        }
    }

}
