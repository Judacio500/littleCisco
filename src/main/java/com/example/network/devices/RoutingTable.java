package com.example.network.devices;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import com.example.network.devices.RouteEntry.RouteType;

public class RoutingTable
{
    private final Map<String, RouteEntry> routes;

    public RoutingTable()
    {
        this.routes = new LinkedHashMap<>();
    }

    public void addOrUpdateRoute(RouteEntry candidate)
    {
        if(candidate == null)
        {
            return;
        }

        String key = candidate.getPrefixKey();
        RouteEntry existing = routes.get(key);

        if(existing == null)
        {
            routes.put(key, candidate);
            return;
        }

        if(existing.getType() == RouteType.CONNECTED && candidate.getType() != RouteType.CONNECTED)
        {
            return;
        }

        if(candidate.getType() == RouteType.CONNECTED)
        {
            routes.put(key, candidate);
            return;
        }

        if(candidate.getType() == RouteType.OSPF)
        {
            routes.put(key, candidate);
            return;
        }

        if(candidate.getType() == RouteType.STATIC && existing.getType() != RouteType.OSPF)
        {
            routes.put(key, candidate);
        }
    }

    public void flushOspfRoutes()
    {
        routes.entrySet().removeIf(entry -> entry.getValue().getType() == RouteType.OSPF);
    }

    public void removeConnectedRoutesForInterface(NIC nic)
    {
        if(nic == null)
        {
            return;
        }
        routes.entrySet().removeIf(entry ->
            entry.getValue().getType() == RouteType.CONNECTED &&
            nic.equals(entry.getValue().getOutInterface())
        );
    }

    public RouteEntry resolve(String destinationIp)
    {
        if(destinationIp == null)
        {
            return null;
        }

        RouteEntry bestMatch = null;
        int longestPrefix = -1;

        for(RouteEntry entry : routes.values())
        {
            if(SubnetUtils.matchesSubnet(destinationIp, entry.getNetworkAddress(), entry.getSubnetMask()))
            {
                int prefix = entry.getPrefixLength();
                if(prefix > longestPrefix)
                {
                    longestPrefix = prefix;
                    bestMatch = entry;
                }
            }
        }

        return bestMatch;
    }

    public Collection<RouteEntry> getAllRoutes()
    {
        return routes.values();
    }

    public boolean isEmpty()
    {
        return routes.isEmpty();
    }
}