package com.example.interfaces;

import com.example.network.devices.NetworkNode;
import com.example.network.packets.IpPacket;

public interface IpProtocolHandler
{
    void handleIpPacket(NetworkNode context, IpPacket packet);
}