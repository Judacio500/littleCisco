package com.example.network;

import java.util.UUID;
import com.example.behaviour.BasicConsole;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;

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
    protected boolean isIpForwardingEnabled()
    {
        return false;
    }

    @Override
    protected void handleIncomingFrame(NIC incomingNic, EthernetFrame frame)
    {
    }

    @Override
    protected void onLocalIpPacket(IpPacket packet)
    {
        if(packet.getProtocol() == Protocol.DATA)
        {
            String message = "Mensaje recibido de " + packet.getSourceIp() + ": " + packet.getPayload().asString();
            console.log("Red", message);
            System.out.println("[" + displayName + "] " + message);
        }
    }
}