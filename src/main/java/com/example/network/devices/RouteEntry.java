package com.example.network.devices;

public class RouteEntry
{
    private final String destinationIp;
    private final String nextHopIp;
    private final NIC outInterface;
    private final int cost;

    public RouteEntry(String destinationIp, String nextHopIp, NIC outInterface, int cost)
    {
        this.destinationIp = destinationIp;
        this.nextHopIp = nextHopIp;
        this.outInterface = outInterface;
        this.cost = cost;
    }

    public static RouteEntry createDirectRoute(String destinationIp, NIC outInterface)
    {
        return new RouteEntry(destinationIp, null, outInterface, 0);
    }

    public boolean isDirectRoute()
    {
        return nextHopIp == null;
    }

    public String getDestinationIp()
    {
        return destinationIp;
    }

    public String getNextHopIp()
    {
        return nextHopIp;
    }

    public NIC getOutInterface()
    {
        return outInterface;
    }

    public int getCost()
    {
        return cost;
    }
}