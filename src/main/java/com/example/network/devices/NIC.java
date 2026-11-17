package com.example.network.devices;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import com.example.network.packets.EthernetFrame;

public class NIC
{
    private final String name;
    private final String macAddress;
    private String ipAddress;
    private NetworkNode hostNode;
    private final LinkedBlockingQueue<EthernetFrame> rxBuffer;
    private final Set<String> subscribedMulticastMacs;

    public NIC(String name, String ipAddress)
    {
        this.name = name;
        this.ipAddress = ipAddress;
        this.macAddress = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.rxBuffer = new LinkedBlockingQueue<>();
        this.subscribedMulticastMacs = new HashSet<>();
    }

    public void attachToNode(NetworkNode hostNode)
    {
        this.hostNode = hostNode;
    }

    public void joinMulticastGroup(String multicastMac)
    {
        subscribedMulticastMacs.add(multicastMac.toUpperCase());
    }

    public void leaveMulticastGroup(String multicastMac)
    {
        subscribedMulticastMacs.remove(multicastMac.toUpperCase());
    }

    public boolean acceptsFrame(EthernetFrame frame)
    {
        if(frame == null || frame.getDestMac() == null)
        {
            return false;
        }
        if(frame.isBroadcast())
        {
            return true;
        }
        String destMac = frame.getDestMac().toUpperCase();
        return macAddress.equals(destMac) || subscribedMulticastMacs.contains(destMac);
    }

    public void receiveFrame(EthernetFrame frame)
    {
        if(!acceptsFrame(frame))
        {
            return;
        }
        rxBuffer.offer(frame);
        if(hostNode != null)
        {
            hostNode.onFrameReceived(this);
        }
    }

    public EthernetFrame pollFrame()
    {
        return rxBuffer.poll();
    }

    public String getName()
    {
        return name;
    }

    public String getMacAddress()
    {
        return macAddress;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public NetworkNode getHostNode()
    {
        return hostNode;
    }
}