package com.example.network.processes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import com.example.interfaces.FrameHandler;
import com.example.network.JGraphTNetwork;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.packets.ArpPacket;
import com.example.network.packets.ArpPacket.Operation;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.FramePayload;

public class ArpProcess implements FrameHandler
{
    private static class PendingTransmission
    {
        private final NIC outNic;
        private final FramePayload payload;

        public PendingTransmission(NIC outNic, FramePayload payload)
        {
            this.outNic = outNic;
            this.payload = payload;
        }
    }

    private final Map<String, Queue<PendingTransmission>> pendingQueue;

    public ArpProcess()
    {
        this.pendingQueue = new HashMap<>();
    }

    public void resolveAndSend(NetworkNode context, NIC outNic, String nextHopIp, FramePayload payload)
    {
        JGraphTNetwork network = context.getNetwork();
        if(network == null || outNic == null || nextHopIp == null)
        {
            return;
        }

        String destMac = context.getArpTable().get(nextHopIp);
        if(destMac != null)
        {
            EthernetFrame frame = new EthernetFrame(outNic.getMacAddress(), destMac, payload);
            network.transmitFrame(outNic, frame);
            return;
        }

        Queue<PendingTransmission> queue = pendingQueue.computeIfAbsent(nextHopIp, k -> new LinkedList<>());
        boolean requestAlreadySent = !queue.isEmpty();
        queue.offer(new PendingTransmission(outNic, payload));

        if(!requestAlreadySent)
        {
            ArpPacket request = ArpPacket.createRequest(outNic.getMacAddress(), outNic.getIpAddress(), nextHopIp);
            EthernetFrame broadcastFrame = new EthernetFrame(outNic.getMacAddress(), EthernetFrame.BROADCAST_MAC, request);
            network.transmitFrame(outNic, broadcastFrame);
        }
    }

    @Override
    public void handleFrame(NetworkNode context, NIC incomingNic, EthernetFrame frame)
    {
        if(!(frame.getPayload() instanceof ArpPacket))
        {
            return;
        }

        ArpPacket arp = (ArpPacket) frame.getPayload();
        if(arp.getSenderIp() != null && arp.getSenderMac() != null)
        {
            context.getArpTable().put(arp.getSenderIp(), arp.getSenderMac());
            flushPendingQueue(context, arp.getSenderIp(), arp.getSenderMac());
        }

        if(arp.getOperation() == Operation.REQUEST)
        {
            if(incomingNic.getIpAddress() != null && incomingNic.getIpAddress().equals(arp.getTargetIp()))
            {
                ArpPacket reply = ArpPacket.createReply(
                    incomingNic.getMacAddress(),
                    incomingNic.getIpAddress(),
                    arp.getSenderMac(),
                    arp.getSenderIp()
                );
                EthernetFrame replyFrame = new EthernetFrame(incomingNic.getMacAddress(), arp.getSenderMac(), reply);
                if(context.getNetwork() != null)
                {
                    context.getNetwork().transmitFrame(incomingNic, replyFrame);
                }
            }
        }
    }

    private void flushPendingQueue(NetworkNode context, String resolvedIp, String resolvedMac)
    {
        Queue<PendingTransmission> queue = pendingQueue.remove(resolvedIp);
        JGraphTNetwork network = context.getNetwork();
        if(queue == null || network == null)
        {
            return;
        }

        while(!queue.isEmpty())
        {
            PendingTransmission pending = queue.poll();
            EthernetFrame frame = new EthernetFrame(pending.outNic.getMacAddress(), resolvedMac, pending.payload);
            network.transmitFrame(pending.outNic, frame);
        }
    }
}