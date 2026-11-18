package com.example.network.devices;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.example.network.packets.EthernetFrame;

public class NIC
{
    private final String name;
    private final String macAddress;
    private String ipAddress;
    private String subnetMask;
    private NetworkNode hostNode;
    private final ConcurrentLinkedQueue<EthernetFrame> rxBuffer;
    private final Set<String> subscribedMulticastMacs;

    public NIC(String name, String ipAddress, String subnetMask)
    {
        this.name = name;
        this.macAddress = generateRandomMac();
        this.ipAddress = ipAddress;
        this.subnetMask = (ipAddress != null && subnetMask == null) ? "255.255.255.0" : subnetMask;
        this.rxBuffer = new ConcurrentLinkedQueue<>();
        this.subscribedMulticastMacs = new HashSet<>();
    }

    public NIC(String name, String ipAddress)
    {
        this(name, ipAddress, ipAddress != null ? "255.255.255.0" : null);
    }

    private String generateRandomMac()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public void attachToNode(NetworkNode node)
    {
        this.hostNode = node;
    }

    public void setIpConfiguration(String ipAddress, String subnetMask)
    {
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        if(hostNode != null)
        {
            hostNode.onInterfaceIpConfigured(this);
        }
    }

    public void joinMulticastGroup(String multicastMac)
    {
        if(multicastMac != null)
        {
            subscribedMulticastMacs.add(multicastMac.toUpperCase());
        }
    }

    public void leaveMulticastGroup(String multicastMac)
    {
        if(multicastMac != null)
        {
            subscribedMulticastMacs.remove(multicastMac.toUpperCase());
        }
    }

    public boolean acceptsFrame(EthernetFrame frame)
    {
        if(frame == null || frame.getDestMac() == null)
        {
            return false;
        }
        String destMac = frame.getDestMac().toUpperCase();
        return destMac.equals(this.macAddress)
            || destMac.equals(EthernetFrame.BROADCAST_MAC)
            || subscribedMulticastMacs.contains(destMac);
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

    public String getSubnetMask()
    {
        return subnetMask;
    }

    public NetworkNode getHostNode()
    {
        return hostNode;
    }
}