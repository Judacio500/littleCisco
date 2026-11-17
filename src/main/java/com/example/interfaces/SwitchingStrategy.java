package com.example.interfaces;

import com.example.network.devices.NetworkNode;
import com.example.network.packets.Payload;

public interface SwitchingStrategy
{
    void send(NetworkNode origin, String destinationIp, Payload payload);
}