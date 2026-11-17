package com.example.network.packets;

import java.util.UUID;

public class IpPacket implements FramePayload
{
    public enum Protocol
    {
        DATA,
        OSPF
    }

    private final UUID packetId;
    private final String sourceIp;
    private final String destIp;
    private int ttl;
    private final Protocol protocol;
    private final int fragmentIndex;
    private final int totalFragments;
    private final Payload payload;

    public IpPacket(UUID packetId, String sourceIp, String destIp, int ttl, Protocol protocol, int fragmentIndex, int totalFragments, Payload payload)
    {
        this.packetId = packetId;
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.ttl = ttl;
        this.protocol = protocol;
        this.fragmentIndex = fragmentIndex;
        this.totalFragments = totalFragments;
        this.payload = payload;
    }

    public IpPacket(String sourceIp, String destIp, Protocol protocol, Payload payload)
    {
        this(UUID.randomUUID(), sourceIp, destIp, 64, protocol, 1, 1, payload);
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

    public UUID getPacketId()
    {
        return packetId;
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

    public int getFragmentIndex()
    {
        return fragmentIndex;
    }

    public int getTotalFragments()
    {
        return totalFragments;
    }

    public Payload getPayload()
    {
        return payload;
    }
}