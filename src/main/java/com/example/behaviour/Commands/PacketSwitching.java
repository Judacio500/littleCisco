package com.example.behaviour.Commands;

import java.util.Arrays;
import java.util.UUID;
import com.example.interfaces.FrameHandler;
import com.example.interfaces.SwitchingStrategy;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.RouteEntry;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.packets.Payload;
import com.example.network.processes.Ipv4Process;

public class PacketSwitching implements SwitchingStrategy
{
    private final int mtuBytes;

    public PacketSwitching(int mtuBytes)
    {
        this.mtuBytes = mtuBytes > 0 ? mtuBytes : 5;
    }

    public PacketSwitching()
    {
        this(5);
    }

    @Override
    public void send(NetworkNode origin, String destinationIp, Payload payload)
    {
        if(origin == null || destinationIp == null || payload == null)
        {
            return;
        }

        origin.queueInstruction(context -> {
            FrameHandler handler = context.getFrameHandler(EtherType.IPV4);
            if(!(handler instanceof Ipv4Process))
            {
                System.out.println("[" + context.getDisplayName() + "] Proceso IPv4 no disponible.");
                return;
            }

            String sourceIp = resolveSourceIp(context, destinationIp);
            if(sourceIp == null)
            {
                System.out.println("[" + context.getDisplayName() + "] Sin ruta hacia " + destinationIp + ".");
                return;
            }

            Ipv4Process ipv4 = (Ipv4Process) handler;
            byte[] rawData = payload.getRawData();
            UUID packetId = UUID.randomUUID();

            if(rawData.length == 0)
            {
                IpPacket emptyPacket = new IpPacket(packetId, sourceIp, destinationIp, 64, Protocol.DATA, 1, 1, new Payload(new byte[0]));
                ipv4.sendIpPacket(context, emptyPacket);
                return;
            }

            int totalFragments = (int) Math.ceil((double) rawData.length / mtuBytes);
            for(int i = 0; i < totalFragments; i++)
            {
                int start = i * mtuBytes;
                int end = Math.min(start + mtuBytes, rawData.length);
                byte[] chunk = Arrays.copyOfRange(rawData, start, end);

                IpPacket fragment = new IpPacket(
                    packetId,
                    sourceIp,
                    destinationIp,
                    64,
                    Protocol.DATA,
                    i + 1,
                    totalFragments,
                    new Payload(chunk)
                );
                ipv4.sendIpPacket(context, fragment);
            }
        });
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
}