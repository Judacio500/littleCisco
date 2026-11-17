package com.example.network.packets;

public class ArpPacket implements FramePayload
{
    public static final String UNKNOWN_MAC = "000000000000";

    public enum Operation
    {
        REQUEST,
        REPLY
    }

    private final Operation operation;
    private final String senderMac;
    private final String senderIp;
    private final String targetMac;
    private final String targetIp;

    public ArpPacket(Operation operation, String senderMac, String senderIp, String targetMac, String targetIp)
    {
        this.operation = operation;
        this.senderMac = senderMac;
        this.senderIp = senderIp;
        this.targetMac = targetMac;
        this.targetIp = targetIp;
    }

    public static ArpPacket createRequest(String senderMac, String senderIp, String targetIp)
    {
        return new ArpPacket(Operation.REQUEST, senderMac, senderIp, UNKNOWN_MAC, targetIp);
    }

    public static ArpPacket createReply(String senderMac, String senderIp, String targetMac, String targetIp)
    {
        return new ArpPacket(Operation.REPLY, senderMac, senderIp, targetMac, targetIp);
    }

    @Override
    public EtherType getEtherType()
    {
        return EtherType.ARP;
    }

    public Operation getOperation()
    {
        return operation;
    }

    public String getSenderMac()
    {
        return senderMac;
    }

    public String getSenderIp()
    {
        return senderIp;
    }

    public String getTargetMac()
    {
        return targetMac;
    }

    public String getTargetIp()
    {
        return targetIp;
    }
}