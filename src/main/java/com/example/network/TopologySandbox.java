package com.example.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.NodeFactory;
import com.example.network.devices.Pc;
import com.example.network.devices.Router;

public class TopologySandbox
{
    private JGraphTNetwork network;
    private final Map<String, NetworkNode> nodes;
    private ExecutorService executor;

    public TopologySandbox()
    {
        this.network = new JGraphTNetwork();
        this.nodes = new LinkedHashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public void registerNode(NetworkNode node)
    {
        if(node == null) return;
        network.addNode(node);
        nodes.put(node.getDisplayName().toUpperCase(), node);
        if(node instanceof Pc) ((Pc) node).console.bind(node, nodes);
        else if(node instanceof Router) ((Router) node).console.bind(node, nodes);
        executor.submit(node);
    }

    public void resetSandbox()
    {
        shutdown();
        this.network = new JGraphTNetwork();
        this.nodes.clear();
        this.executor = Executors.newCachedThreadPool();
    }

    public void runFullVerificationSuite()
    {
        resetSandbox();
        buildDefaultStaticTopology();
        sleepQuietly(300);

        Pc initPcA = (Pc) getNode("PC_A");
        initPcA.console.process("System", "SEND_P PC_B \"Hola multi-salto estatico hacia PC_B\"");
        initPcA.console.process("System", "SEND_P PC_C \"Hola multi-salto estatico hacia PC_C\"");
        initPcA.console.process("System", "CONNECT PC_B");
        sleepQuietly(250);
        initPcA.console.process("System", "SEND_C \"Flujo dedicado por circuito reservado\"");
        sleepQuietly(200);
        initPcA.console.process("System", "DISCONNECT");
        sleepQuietly(200);

        String staticSummary = initPcA.console.process("System", "LOG");
        runOspfFiveRouterSuite(staticSummary);
    }

    public void buildDefaultStaticTopology()
    {
        Pc pcA = NodeFactory.createPc("PC_A");
        NIC nicA = new NIC("eth0", "192.168.1.10", "255.255.255.0");
        pcA.addInterface(nicA);

        Pc pcB = NodeFactory.createPc("PC_B");
        NIC nicB = new NIC("eth0", "192.168.2.10", "255.255.255.0");
        pcB.addInterface(nicB);

        Pc pcC = NodeFactory.createPc("PC_C");
        NIC nicC = new NIC("eth0", "192.168.3.10", "255.255.255.0");
        pcC.addInterface(nicC);

        Router router1 = NodeFactory.createRouter("Router_1");
        NIC r1Eth0 = new NIC("eth0", "192.168.1.1", "255.255.255.0");
        NIC r1Eth1 = new NIC("eth1", "192.168.2.1", "255.255.255.0");
        NIC r1Eth2 = new NIC("eth2", "192.168.3.1", "255.255.255.0");
        router1.addInterface(r1Eth0);
        router1.addInterface(r1Eth1);
        router1.addInterface(r1Eth2);

        registerNode(pcA);
        registerNode(pcB);
        registerNode(pcC);
        registerNode(router1);

        network.addLink(nicA, r1Eth0, new Link("LNK-A-R1"), 10.0);
        network.addLink(r1Eth1, nicB, new Link("LNK-R1-B"), 20.0);
        network.addLink(r1Eth2, nicC, new Link("LNK-R1-C"), 15.0);

        pcA.console.process("System", "ip route 0.0.0.0 0.0.0.0 192.168.1.1 eth0");
        pcB.console.process("System", "ip route 0.0.0.0 0.0.0.0 192.168.2.1 eth0");
        pcC.console.process("System", "ip route 0.0.0.0 0.0.0.0 192.168.3.1 eth0");
    }

    public void runOspfFiveRouterSuite(String previousStaticLog)
    {
        resetSandbox();

        Pc pcA = NodeFactory.createPc("PC_A");
        NIC nicA = new NIC("eth0", "10.0.1.10", "255.255.255.0");
        pcA.addInterface(nicA);

        Pc pcB = NodeFactory.createPc("PC_B");
        NIC nicB = new NIC("eth0", "10.0.4.10", "255.255.255.0");
        pcB.addInterface(nicB);

        Pc pcExt = NodeFactory.createPc("PC_EXT");
        NIC nicExt = new NIC("eth0", "10.0.59.10", "255.255.255.0");
        NIC loExt = new NIC("lo0", "172.16.99.10", "255.255.255.0");
        pcExt.addInterface(nicExt);
        pcExt.addInterface(loExt);

        Router r1 = NodeFactory.createRouter("R1");
        NIC r1Eth0 = new NIC("eth0", "10.0.1.1", "255.255.255.0");
        NIC r1Eth1 = new NIC("eth1", "10.1.12.1", "255.255.255.0");
        NIC r1Eth2 = new NIC("eth2", "10.1.13.1", "255.255.255.0");
        r1.addInterface(r1Eth0);
        r1.addInterface(r1Eth1);
        r1.addInterface(r1Eth2);

        Router r2 = NodeFactory.createRouter("R2");
        NIC r2Eth0 = new NIC("eth0", "10.1.12.2", "255.255.255.0");
        NIC r2Eth1 = new NIC("eth1", "10.0.23.2", "255.255.255.0");
        NIC r2Eth2 = new NIC("eth2", "10.0.24.2", "255.255.255.0");
        r2.addInterface(r2Eth0);
        r2.addInterface(r2Eth1);
        r2.addInterface(r2Eth2);

        Router r3 = NodeFactory.createRouter("R3");
        NIC r3Eth0 = new NIC("eth0", "10.1.13.3", "255.255.255.0");
        NIC r3Eth1 = new NIC("eth1", "10.0.23.3", "255.255.255.0");
        NIC r3Eth2 = new NIC("eth2", "10.0.35.3", "255.255.255.0");
        r3.addInterface(r3Eth0);
        r3.addInterface(r3Eth1);
        r3.addInterface(r3Eth2);

        Router r4 = NodeFactory.createRouter("R4");
        NIC r4Eth0 = new NIC("eth0", "10.0.24.4", "255.255.255.0");
        NIC r4Eth1 = new NIC("eth1", "10.0.4.1", "255.255.255.0");
        r4.addInterface(r4Eth0);
        r4.addInterface(r4Eth1);

        Router r5 = NodeFactory.createRouter("R5");
        NIC r5Eth0 = new NIC("eth0", "10.0.35.5", "255.255.255.0");
        NIC r5Eth1 = new NIC("eth1", "10.0.59.5", "255.255.255.0");
        r5.addInterface(r5Eth0);
        r5.addInterface(r5Eth1);

        registerNode(pcA);
        registerNode(pcB);
        registerNode(pcExt);
        registerNode(r1);
        registerNode(r2);
        registerNode(r3);
        registerNode(r4);
        registerNode(r5);

        network.addLink(nicA, r1Eth0, new Link("LNK-PCA-R1"), 10.0);
        network.addLink(r1Eth1, r2Eth0, new Link("LNK-R1-R2"), 10.0);
        network.addLink(r1Eth2, r3Eth0, new Link("LNK-R1-R3"), 50.0);
        network.addLink(r2Eth1, r3Eth1, new Link("LNK-R2-R3"), 10.0);
        network.addLink(r2Eth2, r4Eth0, new Link("LNK-R2-R4"), 10.0);
        network.addLink(r3Eth2, r5Eth0, new Link("LNK-R3-R5"), 15.0);
        network.addLink(r4Eth1, nicB, new Link("LNK-R4-PCB"), 10.0);
        network.addLink(r5Eth1, nicExt, new Link("LNK-R5-EXT"), 10.0);

        pcA.console.process("System", "ip route 0.0.0.0 0.0.0.0 10.0.1.1 eth0");
        pcB.console.process("System", "ip route 0.0.0.0 0.0.0.0 10.0.4.1 eth0");
        pcExt.console.process("System", "ip route 0.0.0.0 0.0.0.0 10.0.59.5 eth0");

        r1.console.process("System", "router ospf 1");
        r1.console.process("System", "router-id 1.1.1.1");
        r1.console.process("System", "network 10.0.1.0 0.0.0.255 area 1");
        r1.console.process("System", "network 10.1.12.0 0.0.0.255 area 1");
        r1.console.process("System", "network 10.1.13.0 0.0.0.255 area 1");

        r2.console.process("System", "router ospf 1");
        r2.console.process("System", "router-id 2.2.2.2");
        r2.console.process("System", "network 10.1.12.0 0.0.0.255 area 1");
        r2.console.process("System", "network 10.0.23.0 0.0.0.255 area 0");
        r2.console.process("System", "network 10.0.24.0 0.0.0.255 area 0");

        r3.console.process("System", "router ospf 1");
        r3.console.process("System", "router-id 3.3.3.3");
        r3.console.process("System", "network 10.1.13.0 0.0.0.255 area 1");
        r3.console.process("System", "network 10.0.23.0 0.0.0.255 area 0");
        r3.console.process("System", "network 10.0.35.0 0.0.0.255 area 0");

        r4.console.process("System", "router ospf 1");
        r4.console.process("System", "router-id 4.4.4.4");
        r4.console.process("System", "network 10.0.24.0 0.0.0.255 area 0");
        r4.console.process("System", "network 10.0.4.0 0.0.0.255 area 2");

        r5.console.process("System", "ip route 172.16.99.0 255.255.255.0 10.0.59.10 eth1");
        r5.console.process("System", "router ospf 1");
        r5.console.process("System", "router-id 5.5.5.5");
        r5.console.process("System", "network 10.0.35.0 0.0.0.255 area 0");
        r5.console.process("System", "redistribute static");

        sleepQuietly(1500);

        if(previousStaticLog != null && !previousStaticLog.isEmpty())
        {
            pcA.console.log("Suite-1", "=== RESULTADOS PRUEBA INICIAL ESTATICA/CIRCUITOS ===");
            pcA.console.log("Suite-1", previousStaticLog);
        }

        pcA.console.log("OSPF-Suite", "=== CONVERGENCIA OSPF 1.jg (5 ROUTERS: R1, R2, R3, R4, R5) ===");
        pcA.console.log("OSPF-Suite", r1.console.process("System", "show ip ospf neighbor"));
        pcA.console.log("OSPF-Suite", r1.console.process("System", "show ip route"));
        pcA.console.log("OSPF-Suite", r2.console.process("System", "show ip route"));
        pcA.console.log("OSPF-Suite", r1.console.process("System", "show ip ospf database"));

        pcA.console.process("System", "ping 10.0.4.10");
        sleepQuietly(400);

        pcA.console.process("System", "ping 172.16.99.10");
        sleepQuietly(400);

        pcA.console.process("System", "SEND_P PC_B \"Saludos desde Area 1 cruzando Backbone Area 0 hacia Area 2!\"");
        sleepQuietly(400);
    }

    public JGraphTNetwork getNetwork() { return network; }

    public NetworkNode getNode(String name)
    {
        if(name == null) return null;
        return nodes.get(name.trim().toUpperCase());
    }

    public Map<String, NetworkNode> getNodes() { return nodes; }

    private void sleepQuietly(long millis)
    {
        try { Thread.sleep(millis); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void shutdown()
    {
        for(NetworkNode node : nodes.values()) node.stopNode();
        if(executor != null && !executor.isShutdown()) executor.shutdownNow();
    }
}