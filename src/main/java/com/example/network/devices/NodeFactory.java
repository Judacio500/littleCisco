package com.example.network.devices;

import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.processes.ArpProcess;
import com.example.network.processes.CircuitProcess;
import com.example.network.processes.DataProcess;
import com.example.network.processes.Ipv4Process;
import com.example.network.processes.OspfProcess;
import com.example.network.processes.PingProcess;

public final class NodeFactory
{
    private NodeFactory() {}

    public static Pc createPc(String displayName)
    {
        Pc pc = new Pc(displayName);
        installBaseProcesses(pc);
        pc.registerIpHandler(Protocol.DATA, new DataProcess());
        return pc;
    }

    public static Router createRouter(String displayName)
    {
        Router router = new Router(displayName);
        installBaseProcesses(router);
        router.registerIpHandler(Protocol.OSPF, new OspfProcess());
        return router;
    }

    private static void installBaseProcesses(NetworkNode node)
    {
        node.registerFrameHandler(EtherType.ARP, new ArpProcess());
        node.registerFrameHandler(EtherType.IPV4, new Ipv4Process());
        node.registerFrameHandler(EtherType.CIRCUIT, new CircuitProcess());
        node.registerIpHandler(Protocol.PING, new PingProcess());
    }
}