package com.example.network;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.NodeFactory;
import com.example.network.devices.Pc;
import com.example.network.devices.RouteEntry;
import com.example.network.devices.Router;

public class TopologySandbox
{
    private final JGraphTNetwork network;
    private final Map<String, NetworkNode> nodes;
    private final Map<String, String> knownHosts;
    private final ExecutorService executor;

    public TopologySandbox()
    {
        this.network = new JGraphTNetwork();
        this.nodes = new LinkedHashMap<>();
        this.knownHosts = new LinkedHashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public void registerNode(NetworkNode node)
    {
        nodes.put(node.getDisplayName(), node);
        network.addNode(node);

        if(!node.getInterfaces().isEmpty())
        {
            NIC primary = node.getInterfaces().get(0);
            if(primary.getIpAddress() != null)
            {
                knownHosts.put(node.getDisplayName(), primary.getIpAddress());
            }
        }

        refreshAllConsoles();
        executor.submit(node);
    }

    public void refreshAllConsoles()
    {
        for(NetworkNode node : nodes.values())
        {
            if(node instanceof Pc)
            {
                ((Pc) node).console.setup((Pc) node, knownHosts);
            }
        }
    }

    public NetworkNode getNode(String name)
    {
        return nodes.get(name);
    }

    public Collection<NetworkNode> getAllNodes()
    {
        return nodes.values();
    }

    public JGraphTNetwork getNetwork()
    {
        return network;
    }

    public void buildDefaultMultiHopTopology()
    {
        Pc pcA = NodeFactory.createPc("PC_A");
        NIC nicA = new NIC("eth0", "192.168.1.10");
        pcA.addInterface(nicA);

        Pc pcC = NodeFactory.createPc("PC_C");
        NIC nicC = new NIC("eth0", "192.168.1.11");
        pcC.addInterface(nicC);

        Router router1 = NodeFactory.createRouter("Router_1");
        NIC r1Eth0 = new NIC("eth0", "192.168.1.1");
        NIC r1Eth1 = new NIC("eth1", "192.168.2.1");
        router1.addInterface(r1Eth0);
        router1.addInterface(r1Eth1);

        Pc pcB = NodeFactory.createPc("PC_B");
        NIC nicB = new NIC("eth0", "192.168.2.10");
        pcB.addInterface(nicB);

        registerNode(pcA);
        registerNode(pcC);
        registerNode(router1);
        registerNode(pcB);

        network.addLink(nicA, r1Eth0, new Link("LNK-A-R1"), 10.0);
        network.addLink(nicC, r1Eth0, new Link("LNK-C-R1"), 10.0);
        network.addLink(r1Eth1, nicB, new Link("LNK-R1-B"), 20.0);

        pcA.addRoute(RouteEntry.createDirectRoute("192.168.1.1", nicA));
        pcA.addRoute(RouteEntry.createDirectRoute("192.168.1.11", nicA));
        pcA.addRoute(new RouteEntry("0.0.0.0", "192.168.1.1", nicA, 1));

        pcC.addRoute(RouteEntry.createDirectRoute("192.168.1.1", nicC));
        pcC.addRoute(RouteEntry.createDirectRoute("192.168.1.10", nicC));
        pcC.addRoute(new RouteEntry("0.0.0.0", "192.168.1.1", nicC, 1));

        pcB.addRoute(RouteEntry.createDirectRoute("192.168.2.1", nicB));
        pcB.addRoute(new RouteEntry("0.0.0.0", "192.168.2.1", nicB, 1));

        router1.addRoute(RouteEntry.createDirectRoute("192.168.1.10", r1Eth0));
        router1.addRoute(RouteEntry.createDirectRoute("192.168.1.11", r1Eth0));
        router1.addRoute(RouteEntry.createDirectRoute("192.168.2.10", r1Eth1));
    }

    public void runVerificationSuite()
    {
        Pc pcA = (Pc) getNode("PC_A");
        Pc pcB = (Pc) getNode("PC_B");
        Pc pcC = (Pc) getNode("PC_C");
        Router router1 = (Router) getNode("Router_1");

        if(pcA == null || pcB == null || pcC == null || router1 == null)
        {
            System.out.println("Error: Carga primero la topologia por defecto.");
            return;
        }

        System.out.println("\n================ INICIANDO SUITE DE VERIFICACION ================");

        System.out.println("\n[PRUEBA 1] Packet Switching multi-salto + ARP dinamico + Reensamblado UTF-8");
        System.out.println(pcA.console.process("Test", "SEND_P PC_B \"¡Hola multi-salto con eñes y acentos: áéíóú!\""));
        pause(400);

        System.out.println(pcA.console.process("Test", "ARP"));
        System.out.println("\n--- TABLA ARP (Router_1) ---");
        for(Map.Entry<String, String> entry : router1.getArpTable().entrySet())
        {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println("\n[PRUEBA 2] Circuit Switching: Reserva, Contencion (SETUP_FAIL) y Teardown");
        System.out.println(pcA.console.process("Test", "CONNECT PC_B"));
        pause(300);

        System.out.println("-> Intentando circuito concurrente desde PC_C hacia PC_B (debe fallar por enlace ocupado)...");
        System.out.println(pcC.console.process("Test", "CONNECT PC_B"));
        pause(300);

        System.out.println(pcA.console.process("Test", "SEND_C \"Flujo dedicado por circuito reservado\""));
        pause(300);

        System.out.println(pcA.console.process("Test", "DISCONNECT"));
        pause(300);

        System.out.println("\n================ VERIFICACION FINALIZADA ================");
    }

    public void shutdownAll()
    {
        for(NetworkNode node : nodes.values())
        {
            node.stopNode();
        }
        executor.shutdown();
        try
        {
            if(!executor.awaitTermination(2, TimeUnit.SECONDS))
            {
                executor.shutdownNow();
            }
        }
        catch(InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void pause(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}