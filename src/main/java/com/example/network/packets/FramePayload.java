package com.example.network.packets;

public interface FramePayload
{
    enum EtherType
    {
        IPV4,
        ARP,
        CIRCUIT
    }

    EtherType getEtherType();
}