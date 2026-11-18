package com.example.network.devices;

public class RouteEntry
{
    public enum RouteType
    {
        CONNECTED("C", 0),
        OSPF("O", 1),
        OSPF_IA("O IA", 2),
        OSPF_E2("O E2", 3),
        STATIC("S", 4);

        private final String code;
        private final int preference;

        RouteType(String code, int preference)
        {
            this.code = code;
            this.preference = preference;
        }

        public String getCode()
        {
            return code;
        }

        public int getPreference()
        {
            return preference;
        }

        public boolean isOspfDerived()
        {
            return this == OSPF || this == OSPF_IA || this == OSPF_E2;
        }
    }

    private final RouteType type;
    private final String networkAddress;
    private final String subnetMask;
    private final String nextHopIp;
    private final NIC outInterface;
    private final int metric;

    public RouteEntry(RouteType type, String networkAddress, String subnetMask, String nextHopIp, NIC outInterface, int metric)
    {
        this.type = type;
        this.networkAddress = SubnetUtils.getNetworkAddress(networkAddress, subnetMask);
        this.subnetMask = subnetMask;
        this.nextHopIp = nextHopIp;
        this.outInterface = outInterface;
        this.metric = metric;
    }

    public RouteEntry(String destinationIp, String nextHopIp, NIC outInterface, int metric)
    {
        this(
            RouteType.STATIC,
            destinationIp,
            "0.0.0.0".equals(destinationIp) ? "0.0.0.0" : "255.255.255.255",
            nextHopIp,
            outInterface,
            metric
        );
    }

    public static RouteEntry createConnectedRoute(String ipOrNetwork, String subnetMask, NIC outInterface)
    {
        return new RouteEntry(RouteType.CONNECTED, ipOrNetwork, subnetMask, null, outInterface, 0);
    }

    public static RouteEntry createStaticRoute(String networkAddress, String subnetMask, String nextHopIp, NIC outInterface, int metric)
    {
        return new RouteEntry(RouteType.STATIC, networkAddress, subnetMask, nextHopIp, outInterface, metric);
    }

    public static RouteEntry createOspfRoute(String networkAddress, String subnetMask, String nextHopIp, NIC outInterface, int cost)
    {
        return new RouteEntry(RouteType.OSPF, networkAddress, subnetMask, nextHopIp, outInterface, cost);
    }

    public static RouteEntry createOspfInterAreaRoute(String networkAddress, String subnetMask, String nextHopIp, NIC outInterface, int cost)
    {
        return new RouteEntry(RouteType.OSPF_IA, networkAddress, subnetMask, nextHopIp, outInterface, cost);
    }

    public static RouteEntry createOspfExternalRoute(String networkAddress, String subnetMask, String nextHopIp, NIC outInterface, int cost)
    {
        return new RouteEntry(RouteType.OSPF_E2, networkAddress, subnetMask, nextHopIp, outInterface, cost);
    }

    public boolean isDirectRoute()
    {
        return type == RouteType.CONNECTED || nextHopIp == null;
    }

    public int getPrefixLength()
    {
        return SubnetUtils.maskToPrefixLength(subnetMask);
    }

    public String getPrefixKey()
    {
        return networkAddress + "/" + getPrefixLength();
    }

    public RouteType getType()
    {
        return type;
    }

    public String getNetworkAddress()
    {
        return networkAddress;
    }

    public String getDestinationIp()
    {
        return networkAddress;
    }

    public String getSubnetMask()
    {
        return subnetMask;
    }

    public String getNextHopIp()
    {
        return nextHopIp;
    }

    public NIC getOutInterface()
    {
        return outInterface;
    }

    public int getMetric()
    {
        return metric;
    }
}