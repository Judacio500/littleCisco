package com.example.interfaces;

import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.packets.EthernetFrame;

public interface FrameHandler
{
    void handleFrame(NetworkNode context, NIC incomingNic, EthernetFrame frame);
}