package com.example.network.packets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OspfPacket
{
    public enum OspfType
    {
        HELLO,
        LSA
    }

    public enum LsaType
    {
        ROUTER,
        SUMMARY,
        EXTERNAL
    }

    public static class LinkAdvertisement
    {
        private final String networkAddress;
        private final String subnetMask;
        private final String interfaceIp;
        private final int cost;

        public LinkAdvertisement(String networkAddress, String subnetMask, String interfaceIp, int cost)
        {
            this.networkAddress = networkAddress;
            this.subnetMask = subnetMask;
            this.interfaceIp = interfaceIp;
            this.cost = cost;
        }

        public String getNetworkAddress() { return networkAddress; }
        public String getSubnetMask() { return subnetMask; }
        public String getInterfaceIp() { return interfaceIp; }
        public int getCost() { return cost; }
    }

    public static final String NONE_ID = "0.0.0.0";

    private final OspfType type;
    private final LsaType lsaType;
    private final String originRouterId;
    private final String senderInterfaceIp;
    private final int areaId;
    private final int sequenceNumber;
    private final String drRouterId;
    private final String bdrRouterId;
    private final List<String> activeNeighbors;
    private final List<LinkAdvertisement> links;

    public OspfPacket(
        OspfType type,
        LsaType lsaType,
        String originRouterId,
        String senderInterfaceIp,
        int areaId,
        int sequenceNumber,
        String drRouterId,
        String bdrRouterId,
        List<String> activeNeighbors,
        List<LinkAdvertisement> links)
    {
        this.type = type;
        this.lsaType = lsaType;
        this.originRouterId = originRouterId;
        this.senderInterfaceIp = senderInterfaceIp;
        this.areaId = areaId;
        this.sequenceNumber = sequenceNumber;
        this.drRouterId = (drRouterId != null && !drRouterId.isEmpty()) ? drRouterId : NONE_ID;
        this.bdrRouterId = (bdrRouterId != null && !bdrRouterId.isEmpty()) ? bdrRouterId : NONE_ID;
        this.activeNeighbors = (activeNeighbors != null) ? activeNeighbors : new ArrayList<>();
        this.links = (links != null) ? links : new ArrayList<>();
    }

    public static OspfPacket createHello(String routerId, String interfaceIp, int areaId, String drId, String bdrId, List<String> neighbors)
    {
        return new OspfPacket(OspfType.HELLO, null, routerId, interfaceIp, areaId, 0, drId, bdrId, neighbors, null);
    }

    public static OspfPacket createLsa(LsaType lsaType, String routerId, String interfaceIp, int areaId, int seqNum, List<LinkAdvertisement> links)
    {
        return new OspfPacket(OspfType.LSA, lsaType, routerId, interfaceIp, areaId, seqNum, NONE_ID, NONE_ID, null, links);
    }

    public String getLsdbKey()
    {
        return (lsaType != null ? lsaType.name() : "ROUTER") + ":" + originRouterId;
    }

    public Payload toPayload()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("OSPF|")
          .append(type.name()).append("|")
          .append(lsaType != null ? lsaType.name() : "NONE").append("|")
          .append(originRouterId).append("|")
          .append(senderInterfaceIp).append("|")
          .append(areaId).append("|")
          .append(sequenceNumber).append("|")
          .append(drRouterId).append("|")
          .append(bdrRouterId).append("|")
          .append(String.join(",", activeNeighbors)).append("|");

        for(int i = 0; i < links.size(); i++)
        {
            if(i > 0) sb.append(";");
            LinkAdvertisement l = links.get(i);
            sb.append(l.getNetworkAddress()).append(",")
              .append(l.getSubnetMask()).append(",")
              .append(l.getInterfaceIp()).append(",")
              .append(l.getCost());
        }
        return new Payload(sb.toString());
    }

    public static OspfPacket fromPayload(Payload payload)
    {
        if(payload == null) return null;
        String[] parts = payload.asString().split("\\|", -1);
        if(parts.length < 11 || !"OSPF".equals(parts[0])) return null;

        OspfType type = OspfType.valueOf(parts[1]);
        LsaType lsaType = "NONE".equals(parts[2]) ? null : LsaType.valueOf(parts[2]);
        List<String> neighbors = parts[9].isEmpty() ? new ArrayList<>() : Arrays.asList(parts[9].split(","));

        List<LinkAdvertisement> parsedLinks = new ArrayList<>();
        if(!parts[10].isEmpty())
        {
            for(String entry : parts[10].split(";"))
            {
                String[] f = entry.split(",");
                if(f.length == 4)
                {
                    parsedLinks.add(new LinkAdvertisement(f[0], f[1], f[2], Integer.parseInt(f[3])));
                }
            }
        }

        return new OspfPacket(
            type, lsaType, parts[3], parts[4],
            Integer.parseInt(parts[5]), Integer.parseInt(parts[6]),
            parts[7], parts[8], neighbors, parsedLinks
        );
    }

    public OspfType getType() { return type; }
    public LsaType getLsaType() { return lsaType; }
    public String getOriginRouterId() { return originRouterId; }
    public String getSenderInterfaceIp() { return senderInterfaceIp; }
    public int getAreaId() { return areaId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getDrRouterId() { return drRouterId; }
    public String getBdrRouterId() { return bdrRouterId; }
    public List<String> getActiveNeighbors() { return activeNeighbors; }
    public List<LinkAdvertisement> getLinks() { return links; }
}