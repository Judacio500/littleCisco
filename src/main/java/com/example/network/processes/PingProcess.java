package com.example.network.processes;

import java.util.UUID;
import com.example.interfaces.FrameHandler;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.Pc;
import com.example.network.devices.RouteEntry;
import com.example.network.devices.Router;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.packets.Payload;

public class PingProcess implements IpProtocolHandler
{
    public void sendPing(NetworkNode origin, String destinationIp)
    {
        FrameHandler handler = origin.getFrameHandler(EtherType.IPV4);
        if(!(handler instanceof Ipv4Process))
        {
            return;
        }

        String sourceIp = resolveSourceIp(origin, destinationIp);
        if(sourceIp == null)
        {
            logMessage(origin, "PING fallido: sin ruta hacia " + destinationIp + ".");
            return;
        }

        String pingId = UUID.randomUUID().toString().substring(0, 8);
        String body = "ECHO_REQUEST:" + pingId + ":" + System.nanoTime();
        IpPacket request = new IpPacket(sourceIp, destinationIp, Protocol.PING, new Payload(body));
        ((Ipv4Process) handler).sendIpPacket(origin, request);
    }

    @Override
    public void handleIpPacket(NetworkNode context, IpPacket packet)
    {
        String content = packet.getPayload().asString();
        String[] parts = content.split(":", 3);
        if(parts.length < 3)
        {
            return;
        }

        String type = parts[0];
        String pingId = parts[1];
        String timestampStr = parts[2];

        if("ECHO_REQUEST".equals(type))
        {
            FrameHandler handler = context.getFrameHandler(EtherType.IPV4);
            if(handler instanceof Ipv4Process)
            {
                String replyBody = "ECHO_REPLY:" + pingId + ":" + timestampStr;
                IpPacket reply = new IpPacket(packet.getDestIp(), packet.getSourceIp(), Protocol.PING, new Payload(replyBody));
                ((Ipv4Process) handler).sendIpPacket(context, reply);
            }
        }
        else if("ECHO_REPLY".equals(type))
        {
            try
            {
                long sentNanos = Long.parseLong(timestampStr);
                double rttMs = (System.nanoTime() - sentNanos) / 1_000_000.0;
                String msg = String.format("Respuesta ICMP desde %s (id=%s): TTL=%d tiempo=%.2f ms",
                    packet.getSourceIp(), pingId, packet.getTtl(), rttMs);
                logMessage(context, msg);
            }
            catch(NumberFormatException e)
            {
                logMessage(context, "Respuesta ICMP desde " + packet.getSourceIp() + " (id=" + pingId + ")");
            }
        }
    }

    private String resolveSourceIp(NetworkNode context, String destinationIp)
    {
        if(context.ownsIp(destinationIp))
        {
            return destinationIp;
        }
        RouteEntry route = context.resolveRoute(destinationIp);
        if(route == null || route.getOutInterface() == null)
        {
            return null;
        }
        return route.getOutInterface().getIpAddress();
    }

    private void logMessage(NetworkNode context, String msg)
    {
        if(context instanceof Pc)
        {
            ((Pc) context).console.log("Red", msg);
        }
        else if(context instanceof Router)
        {
            ((Router) context).console.log("Red", msg);
        }
        System.out.println("[" + context.getDisplayName() + "] " + msg);
    }
}