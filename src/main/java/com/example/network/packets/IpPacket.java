package com.example.network.packets;

import java.util.UUID;
import java.util.List;
import com.example.network.Link;

public class IpPacket 
{
    private String sourceIp;
    private String destIp;
    private int protocol; // Ej: 6 para texto/TCP, 89 para OSPF
    private Payload data;

    public IpPacket(String sourceIp, String destIp, int protocol, Payload data) 
    {
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.protocol = protocol;
        this.data = data;
    }

    public String getSourceIp() 
    {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) 
    {
        this.sourceIp = sourceIp;
    }

    public String getDestIp() 
    {
        return destIp;
    }

    public void setDestIp(String destIp) 
    {
        this.destIp = destIp;
    }

    public int getProtocol() 
    {
        return protocol;
    }

    public void setProtocol(int protocol) 
    {
        this.protocol = protocol;
    }

    public Payload getData() 
    {
        return data;
    }

    public void setData(Payload data) 
    {
        this.data = data;
    }

    

}
