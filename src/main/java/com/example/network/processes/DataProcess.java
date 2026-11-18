package com.example.network.processes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.Pc;
import com.example.network.packets.IpPacket;

public class DataProcess implements IpProtocolHandler
{
    private final Map<UUID, Map<Integer, byte[]>> reassemblyBuffers;

    public DataProcess()
    {
        this.reassemblyBuffers = new HashMap<>();
    }

    @Override
    public void handleIpPacket(NetworkNode context, IpPacket packet)
    {
        if(packet.getTotalFragments() <= 1)
        {
            deliverMessage(context, packet.getSourceIp(), packet.getPayload().asString());
            return;
        }

        UUID packetId = packet.getPacketId();
        Map<Integer, byte[]> fragments = reassemblyBuffers.computeIfAbsent(packetId, k -> new HashMap<>());
        fragments.put(packet.getFragmentIndex(), packet.getPayload().getRawData());

        if(fragments.size() == packet.getTotalFragments())
        {
            reassemblyBuffers.remove(packetId);
            String fullMessage = reassembleBytes(fragments, packet.getTotalFragments());
            deliverMessage(context, packet.getSourceIp(), fullMessage);
        }
    }

    private String reassembleBytes(Map<Integer, byte[]> fragments, int totalFragments)
    {
        int totalLength = 0;
        for(int i = 1; i <= totalFragments; i++)
        {
            byte[] part = fragments.get(i);
            if(part != null)
            {
                totalLength += part.length;
            }
        }

        byte[] assembled = new byte[totalLength];
        int offset = 0;
        for(int i = 1; i <= totalFragments; i++)
        {
            byte[] part = fragments.get(i);
            if(part != null)
            {
                System.arraycopy(part, 0, assembled, offset, part.length);
                offset += part.length;
            }
        }

        return new String(assembled, StandardCharsets.UTF_8);
    }

    private void deliverMessage(NetworkNode context, String sourceIp, String message)
    {
        String formatted = "Mensaje recibido de " + sourceIp + ": " + message;
        if(context instanceof Pc)
        {
            ((Pc) context).console.log("Red", formatted);
        }
        System.out.println("[" + context.getDisplayName() + "] " + formatted);
    }
}