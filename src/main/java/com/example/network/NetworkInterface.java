package com.example.network;

import java.util.UUID;

public class NetworkInterface 
{
    private String name; // ej. "eth0"
    private String macAddress; // Cada puerto real tiene su propia MAC en hardware
    private Link connectedLink; // El cable físico de JGraphT conectado a este puerto
    
    public NetworkInterface(String name, Link connectedLink) 
    {
        this.name = name;
        this.macAddress = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.connectedLink = connectedLink;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getMacAddress() 
    {
        return macAddress;
    }

    public void setMacAddress(String macAddress) 
    {
        this.macAddress = macAddress;
    }

    public Link getConnectedLink() 
    {
        return connectedLink;
    }

    public void setConnectedLink(Link connectedLink) 
    {
        this.connectedLink = connectedLink;
    }

    
}
