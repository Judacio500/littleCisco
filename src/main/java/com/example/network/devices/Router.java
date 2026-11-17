package com.example.network.devices;

public package com.example.network;

import com.example.network.packets.EthernetFrame;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;

public class Router extends NetworkNode
{
    public Router(String displayName)
    {
        super(displayName);
    }

    @Override
    protected boolean isIpForwardingEnabled()
    {
        return true;
    }

    @Override
    protected void handleIncomingFrame(NIC incomingNic, EthernetFrame frame)
    {
    }

    @Override
    protected void onLocalIpPacket(IpPacket packet)
    {
        if(packet.getProtocol() == Protocol.OSPF)
        {
            System.out.println("[" + displayName + "] Paquete de control OSPF recibido desde " + packet.getSourceIp());
        }
        else if(packet.getProtocol() == Protocol.DATA)
        {
            System.out.println("[" + displayName + "] Mensaje de diagnostico recibido de " + packet.getSourceIp() + ": " + packet.getPayload().asString());
        }
    }
} {
    
}
