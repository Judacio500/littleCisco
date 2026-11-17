package com.example.network.packets;

public class IpPacket implements FramePayload
{
    public enum Protocol
    {
        DATA,
        OSPF
    }

    private final String sourceIp;
    private final String destIp;
    private int ttl;
    private final Protocol protocol;
    private final Payload payload;

    public IpPacket(String sourceIp, String destIp, int ttl, Protocol protocol, Payload payload)
    {
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.ttl = ttl;
        this.protocol = protocol;
        this.payload = payload;
    }

    public IpPacket(String sourceIp, String destIp, Protocol protocol, Payload payload)
    {
        this(sourceIp, destIp, 64, protocol, payload);
    }

    @Override
    public EtherType getEtherType()
    {
        return EtherType.IPV4;
    }

    public void decrementTtl()
    {
        this.ttl--;
    }

    public boolean isAlive()
    {
        return this.ttl > 0;
    }

    public String getSourceIp()
    {
        return sourceIp;
    }

    public String getDestIp()
    {
        return destIp;
    }

    public int getTtl()
    {
        return ttl;
    }

    public Protocol getProtocol()
    {
        return protocol;
    }

    public Payload getPayload()
    {
        return payload;
    }
}