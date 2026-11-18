package com.example.interfaces;

import com.example.network.devices.NetworkNode;

@FunctionalInterface
public interface Event
{
    void execute(NetworkNode context);
}