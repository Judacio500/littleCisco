package com.example.network.processes;

import com.example.interfaces.FrameHandler;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.JGraphTNetwork;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.RouteEntry;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket;

public class Ipv4Process implements FrameHandler
{
    @Override
    public void handleFrame(NetworkNode context, NIC incomingNic, EthernetFrame frame)
    {
        if(!(frame.getPayload() instanceof IpPacket))
        {
            return;
        }

        IpPacket packet = (IpPacket) frame.getPayload();
        String destIp = packet.getDestIp();

        if(context.ownsIp(destIp) || isMulticastIp(destIp))
        {
            deliverLocally(context, packet);
            return;
        }

        if(!context.isIpForwardingEnabled())
        {
            return;
        }

        packet.decrementTtl();
        if(!packet.isAlive())
        {
            System.out.println("[" + context.getDisplayName() + "] TTL expirado para paquete hacia " + destIp + ".");
            return;
        }

        sendIpPacket(context, packet);
    }

    public void sendIpPacket(NetworkNode context, IpPacket packet)
    {
        String destIp = packet.getDestIp();
        if(context.ownsIp(destIp))
        {
            deliverLocally(context, packet);
            return;
        }

        RouteEntry route = context.resolveRoute(destIp);
        if(route == null)
        {
            System.out.println("[" + context.getDisplayName() + "] Sin ruta hacia " + destIp + ". Paquete descartado.");
            return;
        }

        String nextHopIp = route.isDirectRoute() ? destIp : route.getNextHopIp();
        FrameHandler arpHandler = context.getFrameHandler(EtherType.ARP);
        if(arpHandler instanceof ArpProcess)
        {
            ((ArpProcess) arpHandler).resolveAndSend(context, route.getOutInterface(), nextHopIp, packet);
        }
        else
        {
            System.out.println("[" + context.getDisplayName() + "] Proceso ARP no disponible en el nodo.");
        }
    }

    public void sendMulticastPacket(NetworkNode context, NIC outNic, String multicastMac, IpPacket packet)
    {
        JGraphTNetwork network = context.getNetwork();
        if(network == null || outNic == null)
        {
            return;
        }
        EthernetFrame frame = new EthernetFrame(outNic.getMacAddress(), multicastMac, packet);
        network.transmitFrame(outNic, frame);
    }

    private void deliverLocally(NetworkNode context, IpPacket packet)
    {
        IpProtocolHandler handler = context.getIpHandler(packet.getProtocol());
        if(handler != null)
        {
            handler.handleIpPacket(context, packet);
        }
    }

    private boolean isMulticastIp(String ipAddress)
    {
        return ipAddress != null && ipAddress.startsWith("224.");
    }
}