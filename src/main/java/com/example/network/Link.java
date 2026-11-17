package com.example.network;

import java.util.UUID;
import org.jgrapht.graph.DefaultWeightedEdge;
import com.example.network.devices.NIC;

public class Link extends DefaultWeightedEdge
{
    private final String id;
    private boolean available;
    private UUID circuitId;
    private NIC portA;
    private NIC portB;

    public Link(String id)
    {
        this.id = id;
        this.available = true;
        this.circuitId = null;
    }

    public void bindPorts(NIC portA, NIC portB)
    {
        this.portA = portA;
        this.portB = portB;
    }

    public boolean hasPort(NIC nic)
    {
        if(nic == null)
        {
            return false;
        }
        return nic.equals(portA) || nic.equals(portB);
    }

    public NIC getOtherPort(NIC nic)
    {
        if(nic == null)
        {
            return null;
        }
        if(nic.equals(portA))
        {
            return portB;
        }
        if(nic.equals(portB))
        {
            return portA;
        }
        return null;
    }

    public boolean isAvailable()
    {
        return available && circuitId == null;
    }

    public boolean isPhysicallyUp()
    {
        return available;
    }

    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    public void reserveCircuit(UUID circuitId)
    {
        this.circuitId = circuitId;
    }

    public void releaseCircuit()
    {
        this.circuitId = null;
    }

    public boolean isReservedFor(UUID token)
    {
        return available && token != null && token.equals(circuitId);
    }

    public UUID getCircuitId()
    {
        return circuitId;
    }

    public String getId()
    {
        return id;
    }

    public NIC getPortA()
    {
        return portA;
    }

    public NIC getPortB()
    {
        return portB;
    }

    public double getLinkWeight()
    {
        return super.getWeight();
    }
}