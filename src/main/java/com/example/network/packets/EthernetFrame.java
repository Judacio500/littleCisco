package com.example.network.packets;

public class EthernetFrame 
{
    private String sourceMac;
    private String destMac;
    private IpPacket payload;

    public EthernetFrame(String sourceMac, String destMac, IpPacket payload) 
    {
        this.sourceMac = sourceMac;
        this.destMac = destMac;
        this.payload = payload;
    }

    public String getSourceMac() 
    {
        return sourceMac;
    }

    public void setSourceMac(String sourceMac) 
    {
        this.sourceMac = sourceMac;
    }

    public String getDestMac() 
    {
        return destMac;
    }

    public void setDestMac(String destMac) 
    {
        this.destMac = destMac;
    }

    public IpPacket getPayload() 
    {
        return payload;
    }

    public void setPayload(IpPacket payload) 
    {
        this.payload = payload;
    }

    
}
