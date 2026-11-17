package com.example.network;

import java.util.Set;
import java.util.UUID;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.packets.EthernetFrame;


public class JGraphTNetwork
{
    private final SimpleWeightedGraph<NetworkNode, Link> network;

    public JGraphTNetwork()
    {
        this.network = new SimpleWeightedGraph<>(Link.class);
    }

    public void addNode(NetworkNode node)
    {
        if(node != null)
        {
            network.addVertex(node);
            node.setNetwork(this);
        }
    }

    public void addLink(NIC portA, NIC portB, Link link, double weight)
    {
        if(portA == null || portB == null || link == null)
        {
            return;
        }
        NetworkNode nodeA = portA.getHostNode();
        NetworkNode nodeB = portB.getHostNode();
        if(nodeA == null || nodeB == null || nodeA.equals(nodeB))
        {
            return;
        }

        addNode(nodeA);
        addNode(nodeB);
        link.bindPorts(portA, portB);
        network.addEdge(nodeA, nodeB, link);
        network.setEdgeWeight(link, weight);
    }

    public void transmitFrame(NIC outNic, EthernetFrame frame)
    {
        if(outNic == null || frame == null || outNic.getHostNode() == null)
        {
            return;
        }
        NetworkNode sender = outNic.getHostNode();
        if(!network.containsVertex(sender))
        {
            return;
        }

        Set<Link> connectedLinks = network.edgesOf(sender);
        for(Link link : connectedLinks)
        {
            if(!link.hasPort(outNic) || !link.isAvailable())
            {
                continue;
            }
            NIC remoteNic = link.getOtherPort(outNic);
            if(remoteNic != null)
            {
                remoteNic.receiveFrame(frame);
            }
        }
    }

    public void transmitCircuitFrame(NIC outNic, EthernetFrame frame, UUID circuitToken)
    {
        if(outNic == null || frame == null || circuitToken == null || outNic.getHostNode() == null)
        {
            return;
        }
        NetworkNode sender = outNic.getHostNode();
        if(!network.containsVertex(sender))
        {
            return;
        }

        Set<Link> connectedLinks = network.edgesOf(sender);
        for(Link link : connectedLinks)
        {
            if(!link.hasPort(outNic) || !link.isReservedFor(circuitToken))
            {
                continue;
            }
            NIC remoteNic = link.getOtherPort(outNic);
            if(remoteNic != null)
            {
                remoteNic.receiveFrame(frame);
            }
        }
    }

    public Link getLinkOnPort(NIC port)
    {
        if(port == null || port.getHostNode() == null)
        {
            return null;
        }
        NetworkNode node = port.getHostNode();
        if(!network.containsVertex(node))
        {
            return null;
        }
        for(Link link : network.edgesOf(node))
        {
            if(link.hasPort(port))
            {
                return link;
            }
        }
        return null;
    }

    public SimpleWeightedGraph<NetworkNode, Link> getGraph()
    {
        return network;
    }

    @Override
    public String toString()
    {
        return network.toString();
    }
}