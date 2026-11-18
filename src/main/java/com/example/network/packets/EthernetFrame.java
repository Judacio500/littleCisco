package com.example.network.packets;

import com.example.network.packets.FramePayload.EtherType;

public class EthernetFrame
{
    public static final String BROADCAST_MAC = "FFFFFFFFFFFF";
    public static final String MULTICAST_PREFIX = "01005E";

    private final String sourceMac;
    private final String destMac;
    private final EtherType etherType;
    private final FramePayload payload;

    public EthernetFrame(String sourceMac, String destMac, FramePayload payload)
    {
        this.sourceMac = sourceMac;
        this.destMac = destMac;
        this.payload = payload;
        this.etherType = payload.getEtherType();
    }

    public boolean isBroadcast()
    {
        return BROADCAST_MAC.equalsIgnoreCase(destMac);
    }

    public boolean isMulticast()
    {
        if(destMac == null)
        {
            return false;
        }
        return destMac.toUpperCase().startsWith(MULTICAST_PREFIX);
    }

    public String getSourceMac()
    {
        return sourceMac;
    }

    public String getDestMac()
    {
        return destMac;
    }

    public EtherType getEtherType()
    {
        return etherType;
    }

    public FramePayload getPayload()
    {
        return payload;
    }
}