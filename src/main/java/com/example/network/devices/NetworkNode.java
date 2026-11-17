package com.example.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import com.example.interfaces.Event;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.IpPacket;

public abstract class NetworkNode implements Runnable
{
    protected final UUID nodeId;
    protected String displayName;
    protected final List<NIC> interfaces;
    protected final Map<String, String> arpTable;
    protected final Map<String, RouteEntry> routingTable;
    protected final Map<UUID, NIC> circuitTable;
    protected final LinkedBlockingQueue<Event> listener;
    protected volatile boolean active;

    public NetworkNode(String displayName)
    {
        this.nodeId = UUID.randomUUID();
        this.displayName = displayName;
        this.interfaces = new ArrayList<>();
        this.arpTable = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.circuitTable = new HashMap<>();
        this.listener = new LinkedBlockingQueue<>();
        this.active = true;
    }

    public void addInterface(NIC nic)
    {
        nic.attachToNode(this);
        interfaces.add(nic);
    }

    public NIC getInterfaceByName(String name)
    {
        for(NIC nic : interfaces)
        {
            if(nic.getName().equalsIgnoreCase(name))
            {
                return nic;
            }
        }
        return null;
    }

    public NIC getInterfaceByIp(String ipAddress)
    {
        for(NIC nic : interfaces)
        {
            if(nic.getIpAddress() != null && nic.getIpAddress().equals(ipAddress))
            {
                return nic;
            }
        }
        return null;
    }

    public boolean ownsIp(String ipAddress)
    {
        return getInterfaceByIp(ipAddress) != null;
    }

    public void addRoute(RouteEntry entry)
    {
        routingTable.put(entry.getDestinationIp(), entry);
    }

    public void onFrameReceived(NIC incomingNic)
    {
        queueInstruction(context -> processIncomingNicBuffer(incomingNic));
    }

    protected void processIncomingNicBuffer(NIC incomingNic)
    {
        EthernetFrame frame = incomingNic.pollFrame();
        if(frame != null)
        {
            handleIncomingFrame(incomingNic, frame);
        }
    }

    public void queueInstruction(Event event)
    {
        try
        {
            listener.put(event);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public void stopNode()
    {
        this.active = false;
        queueInstruction(context -> {});
    }

    @Override
    public void run()
    {
        while(active)
        {
            try
            {
                Event event = listener.take();
                if(!active)
                {
                    break;
                }
                try
                {
                    event.execute(this);
                }
                catch(Exception logicException)
                {
                    System.out.println(displayName + " fallo al ejecutar un evento: " + logicException.getMessage());
                }
            }
            catch(InterruptedException e)
            {
                System.out.println("Tarea interrumpida abruptamente en " + displayName + ".");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    protected abstract boolean isIpForwardingEnabled();

    protected abstract void handleIncomingFrame(NIC incomingNic, EthernetFrame frame);

    protected abstract void onLocalIpPacket(IpPacket packet);

    public UUID getNodeId()
    {
        return nodeId;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public List<NIC> getInterfaces()
    {
        return interfaces;
    }

    public Map<String, String> getArpTable()
    {
        return arpTable;
    }

    public Map<String, RouteEntry> getRoutingTable()
    {
        return routingTable;
    }

    public Map<UUID, NIC> getCircuitTable()
    {
        return circuitTable;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(obj == null)
        {
            return false;
        }
        if(getClass() != obj.getClass())
        {
            return false;
        }
        NetworkNode other = (NetworkNode) obj;
        if(nodeId == null)
        {
            if(other.nodeId != null)
            {
                return false;
            }
        }
        else if(!nodeId.equals(other.nodeId))
        {
            return false;
        }
        return true;
    }
}