package com.example.behaviour.Events;

import com.example.interfaces.Event;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.Router;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.processes.OspfProcess;

public class OspfTickEvent implements Event
{
    @Override
    public void execute(NetworkNode context)
    {
        if(context instanceof Router && ((Router) context).isOspfEnabled())
        {
            IpProtocolHandler handler = context.getIpHandler(Protocol.OSPF);
            if(handler instanceof OspfProcess)
            {
                ((OspfProcess) handler).onTick((Router) context);
            }
        }
    }
}