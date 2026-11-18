package com.example.network.devices;

public final class SubnetUtils
{
    private SubnetUtils()
    {
    }

    public static int ipToInt(String ipAddress)
    {
        String[] octets = ipAddress.trim().split("\\.");
        if(octets.length != 4)
        {
            throw new IllegalArgumentException("Direccion IPv4 invalida: " + ipAddress);
        }

        int result = 0;
        for(int i = 0; i < 4; i++)
        {
            int octet = Integer.parseInt(octets[i]);
            if(octet < 0 || octet > 255)
            {
                throw new IllegalArgumentException("Octeto fuera de rango en: " + ipAddress);
            }
            result = (result << 8) | (octet & 0xFF);
        }
        return result;
    }

    public static String intToIp(int ip)
    {
        return ((ip >>> 24) & 0xFF) + "." +
               ((ip >>> 16) & 0xFF) + "." +
               ((ip >>> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    public static String normalizeMask(String ipOrNetwork, String maskOrWildcard)
    {
        if("0.0.0.0".equals(ipOrNetwork) && "255.255.255.255".equals(maskOrWildcard))
        {
            return "0.0.0.0";
        }

        int raw = ipToInt(maskOrWildcard);
        int inverted = ~raw;
        boolean isContiguousSubnetMask = (raw != 0) && ((inverted == 0) || (((inverted + 1) & inverted) == 0));

        if(isContiguousSubnetMask)
        {
            return maskOrWildcard;
        }
        return intToIp(inverted);
    }

    public static String getNetworkAddress(String ipAddress, String subnetMask)
    {
        int ip = ipToInt(ipAddress);
        int mask = ipToInt(subnetMask);
        return intToIp(ip & mask);
    }

    public static boolean matchesSubnet(String targetIp, String networkAddress, String subnetMask)
    {
        if(targetIp == null || networkAddress == null || subnetMask == null)
        {
            return false;
        }
        int target = ipToInt(targetIp);
        int network = ipToInt(networkAddress);
        int mask = ipToInt(subnetMask);
        return (target & mask) == (network & mask);
    }

    public static int maskToPrefixLength(String subnetMask)
    {
        return Integer.bitCount(ipToInt(subnetMask));
    }

    public static boolean isValidIp(String ipAddress)
    {
        if(ipAddress == null)
        {
            return false;
        }
        try
        {
            ipToInt(ipAddress);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
}