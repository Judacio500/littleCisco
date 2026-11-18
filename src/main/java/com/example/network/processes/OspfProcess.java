package com.example.network.processes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import com.example.behaviour.Events.OspfTickEvent;
import com.example.interfaces.FrameHandler;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.Link;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.RouteEntry;
import com.example.network.devices.RouteEntry.RouteType;
import com.example.network.devices.Router;
import com.example.network.devices.SubnetUtils;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.packets.OspfPacket;
import com.example.network.packets.OspfPacket.LinkAdvertisement;
import com.example.network.packets.OspfPacket.LsaType;
import com.example.network.packets.OspfPacket.OspfType;

public class OspfProcess implements IpProtocolHandler
{
    public static final String ALL_SPF_IP = "224.0.0.5";
    public static final String ALL_SPF_MAC = "01005E000005";
    public static final String ALL_DR_IP = "224.0.0.6";
    public static final String ALL_DR_MAC = "01005E000006";

    private static final long HELLO_INTERVAL_MS = 3000L;
    private static final long DEAD_INTERVAL_MS = 12000L;

    public enum NeighborFsm { DOWN, INIT, FULL }

    private static class NetworkRule
    {
        final String network;
        final String mask;
        final int areaId;

        NetworkRule(String net, String mask, int areaId)
        {
            this.network = SubnetUtils.getNetworkAddress(net, mask);
            this.mask = mask;
            this.areaId = areaId;
        }
    }

    private static class InterfaceState
    {
        final NIC nic;
        int areaId;
        String drId = OspfPacket.NONE_ID;
        String bdrId = OspfPacket.NONE_ID;
        long lastHelloSent = 0L;

        InterfaceState(NIC nic, int areaId)
        {
            this.nic = nic;
            this.areaId = areaId;
        }
    }

    private static class NeighborState
    {
        final String routerId;
        String neighborIp;
        final NIC localNic;
        final int areaId;
        NeighborFsm state = NeighborFsm.INIT;
        long lastHeard = System.currentTimeMillis();

        NeighborState(String routerId, String neighborIp, NIC localNic, int areaId)
        {
            this.routerId = routerId;
            this.neighborIp = neighborIp;
            this.localNic = localNic;
            this.areaId = areaId;
        }
    }

    private int processId = 1;
    private String configuredRouterId;
    private boolean redistributeStatic = false;
    private int sequenceNumber = 0;

    private final List<NetworkRule> networkRules = new ArrayList<>();
    private final Map<String, InterfaceState> activeInterfaces = new LinkedHashMap<>();
    private final Map<String, NeighborState> neighbors = new LinkedHashMap<>();
    private final Map<Integer, Map<String, OspfPacket>> areaLsdb = new LinkedHashMap<>();
    private ScheduledExecutorService scheduler;

    public void start(Router router, int processId)
    {
        if(processId > 0) this.processId = processId;
        bindMatchingInterfaces(router);

        if(scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OSPF-1jg-" + router.getDisplayName());
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> router.queueInstruction(new OspfTickEvent()), 0L, 1000L, TimeUnit.MILLISECONDS);
    }

    public void stop()
    {
        if(scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
    }

    public void setRouterId(Router router, String routerId)
    {
        this.configuredRouterId = routerId;
        onTopologyChanged(router);
    }

    public void addNetworkStatement(Router router, String net, String maskOrWildcard, int areaId)
    {
        String mask = SubnetUtils.normalizeMask(net, maskOrWildcard);
        networkRules.add(new NetworkRule(net, mask, areaId));
        onTopologyChanged(router);
    }

    public void setRedistributeStatic(Router router, boolean enabled)
    {
        this.redistributeStatic = enabled;
        if(!enabled)
        {
            String selfId = resolveRouterId(router);
            for(Map<String, OspfPacket> db : areaLsdb.values())
            {
                db.remove(LsaType.EXTERNAL.name() + ":" + selfId);
            }
        }
        onTopologyChanged(router);
    }

    public void onTopologyChanged(Router router)
    {
        if(router == null || !router.isOspfEnabled()) return;
        bindMatchingInterfaces(router);
        originateAndFlood(router);
        recalculateSpf(router);
    }

    private void bindMatchingInterfaces(Router router)
    {
        for(NIC nic : router.getInterfaces())
        {
            if(nic.getIpAddress() == null) continue;
            for(NetworkRule rule : networkRules)
            {
                if(SubnetUtils.matchesSubnet(nic.getIpAddress(), rule.network, rule.mask))
                {
                    String key = nic.getName().toLowerCase();
                    InterfaceState iface = activeInterfaces.computeIfAbsent(key, k -> new InterfaceState(nic, rule.areaId));
                    iface.areaId = rule.areaId;
                    nic.joinMulticastGroup(ALL_SPF_MAC);
                    areaLsdb.computeIfAbsent(rule.areaId, k -> new LinkedHashMap<>());
                    break;
                }
            }
        }
    }

    public void onTick(Router router)
    {
        long now = System.currentTimeMillis();
        boolean changed = false;

        Iterator<NeighborState> it = neighbors.values().iterator();

        while(it.hasNext())
        {
            NeighborState n = it.next();
            if(now - n.lastHeard > DEAD_INTERVAL_MS)
            {
                System.out.println("[" + router.getDisplayName() + "] OSPF vecino caido: " + n.routerId);
                it.remove();
                Map<String, OspfPacket> db = areaLsdb.get(n.areaId);
                if(db != null)
                {
                    db.remove(LsaType.ROUTER.name() + ":" + n.routerId);
                    db.remove(LsaType.SUMMARY.name() + ":" + n.routerId);
                    db.remove(LsaType.EXTERNAL.name() + ":" + n.routerId);
                }
                InterfaceState iface = activeInterfaces.get(n.localNic.getName().toLowerCase());
                if(iface != null && (n.routerId.equals(iface.drId) || n.routerId.equals(iface.bdrId)))
                {
                    if(n.routerId.equals(iface.drId))
                    {
                        iface.drId = OspfPacket.NONE_ID;
                        System.out.println("[" + router.getDisplayName() + "] Failover OSPF en " + iface.nic.getName() + ": BDR asume rol de DR.");
                    }
                    else
                    {
                        iface.bdrId = OspfPacket.NONE_ID;
                    }
                    electDrBdr(router, iface);
                }
                changed = true;
            }
        }

        for(InterfaceState iface : activeInterfaces.values())
        {
            if(now - iface.lastHelloSent >= HELLO_INTERVAL_MS)
            {
                sendHello(router, iface);
                iface.lastHelloSent = now;
            }
        }

        if(changed)
        {
            originateAndFlood(router);
            recalculateSpf(router);
        }
    }

    @Override
    public void handleIpPacket(NetworkNode context, IpPacket packet)
    {
        if(!(context instanceof Router) || !((Router) context).isOspfEnabled()) return;
        Router router = (Router) context;

        OspfPacket ospf = OspfPacket.fromPayload(packet.getPayload());
        if(ospf == null || resolveRouterId(router).equals(ospf.getOriginRouterId())) return;

        NIC inNic = resolveIncomingNic(router, packet.getSourceIp());
        if(inNic == null) return;

        InterfaceState iface = activeInterfaces.get(inNic.getName().toLowerCase());
        if(iface == null || iface.areaId != ospf.getAreaId()) return;

        if(ospf.getType() == OspfType.HELLO) handleHello(router, iface, ospf);
        else if(ospf.getType() == OspfType.LSA) handleLsa(router, iface, ospf);
    }

    private void handleHello(Router router, InterfaceState iface, OspfPacket hello)
    {
        String selfId = resolveRouterId(router);
        String nId = hello.getOriginRouterId();
        String key = iface.nic.getName().toLowerCase() + ":" + nId;

        NeighborState n = neighbors.get(key);
        boolean isNew = (n == null);
        if(isNew)
        {
            n = new NeighborState(nId, hello.getSenderInterfaceIp(), iface.nic, iface.areaId);
            neighbors.put(key, n);
        }
        else
        {
            n.neighborIp = hello.getSenderInterfaceIp();
            n.lastHeard = System.currentTimeMillis();
        }

        if(OspfPacket.NONE_ID.equals(iface.drId) && !OspfPacket.NONE_ID.equals(hello.getDrRouterId()))
        {
            iface.drId = hello.getDrRouterId();
        }
        if(OspfPacket.NONE_ID.equals(iface.bdrId) && !OspfPacket.NONE_ID.equals(hello.getBdrRouterId()) && !hello.getBdrRouterId().equals(iface.drId))
        {
            iface.bdrId = hello.getBdrRouterId();
        }

        boolean becameFull = false;
        if(hello.getActiveNeighbors().contains(selfId) && n.state != NeighborFsm.FULL)
        {
            n.state = NeighborFsm.FULL;
            becameFull = true;
            System.out.println("[" + router.getDisplayName() + "] OSPF adyacencia FULL con " + nId + " (Area " + iface.areaId + ")");
            if(OspfPacket.NONE_ID.equals(iface.drId) || OspfPacket.NONE_ID.equals(iface.bdrId))
            {
                electDrBdr(router, iface);
            }
        }
        syncDrMulticast(router, iface);

        if(isNew)
        {
            sendHello(router, iface);
            iface.lastHelloSent = System.currentTimeMillis();
        }
        if(becameFull)
        {
            originateAndFlood(router);
            recalculateSpf(router);
        }
    }

    private void electDrBdr(Router router, InterfaceState iface)
    {
        List<String> candidates = new ArrayList<>();
        candidates.add(resolveRouterId(router));
        for(NeighborState n : neighbors.values())
        {
            if(n.localNic.equals(iface.nic) && n.state != NeighborFsm.DOWN) candidates.add(n.routerId);
        }
        candidates.sort(Comparator.reverseOrder());

        if(OspfPacket.NONE_ID.equals(iface.drId))
        {
            if(!OspfPacket.NONE_ID.equals(iface.bdrId))
            {
                iface.drId = iface.bdrId;
                iface.bdrId = OspfPacket.NONE_ID;
            }
            else
            {
                iface.drId = candidates.get(0);
            }
        }
        if(OspfPacket.NONE_ID.equals(iface.bdrId))
        {
            for(String c : candidates)
            {
                if(!c.equals(iface.drId))
                {
                    iface.bdrId = c;
                    break;
                }
            }
        }
        syncDrMulticast(router, iface);
    }

    private void syncDrMulticast(Router router, InterfaceState iface)
    {
        String selfId = resolveRouterId(router);
        if(selfId.equals(iface.drId) || selfId.equals(iface.bdrId)) iface.nic.joinMulticastGroup(ALL_DR_MAC);
        else iface.nic.leaveMulticastGroup(ALL_DR_MAC);
    }

    private void handleLsa(Router router, InterfaceState inIface, OspfPacket lsa)
    {
        Map<String, OspfPacket> db = areaLsdb.computeIfAbsent(inIface.areaId, k -> new LinkedHashMap<>());
        OspfPacket prev = db.get(lsa.getLsdbKey());

        if(prev == null || lsa.getSequenceNumber() > prev.getSequenceNumber())
        {
            db.put(lsa.getLsdbKey(), lsa);
            boolean iAmDr = resolveRouterId(router).equals(inIface.drId);

            for(InterfaceState out : activeInterfaces.values())
            {
                if(out.areaId != inIface.areaId) continue;
                if(out.nic.equals(inIface.nic) && !iAmDr) continue;
                sendLsa(router, out, lsa);
            }
            recalculateSpf(router);
        }
    }

    private void originateAndFlood(Router router)
    {
        String selfId = resolveRouterId(router);
        sequenceNumber++;

        for(Integer areaId : getActiveAreas())
        {
            List<LinkAdvertisement> links = new ArrayList<>();
            String primaryIp = null;
            for(InterfaceState iface : activeInterfaces.values())
            {
                if(iface.areaId == areaId && iface.nic.getIpAddress() != null)
                {
                    if(primaryIp == null) primaryIp = iface.nic.getIpAddress();
                    String net = SubnetUtils.getNetworkAddress(iface.nic.getIpAddress(), iface.nic.getSubnetMask());
                    links.add(new LinkAdvertisement(net, iface.nic.getSubnetMask(), iface.nic.getIpAddress(), getLinkCost(router, iface.nic)));
                }
            }
            if(primaryIp != null)
            {
                OspfPacket lsa = OspfPacket.createLsa(LsaType.ROUTER, selfId, primaryIp, areaId, sequenceNumber, links);
                areaLsdb.computeIfAbsent(areaId, k -> new LinkedHashMap<>()).put(lsa.getLsdbKey(), lsa);
                floodArea(router, areaId, lsa);
            }
        }

        if(redistributeStatic)
        {
            List<LinkAdvertisement> extLinks = new ArrayList<>();
            for(RouteEntry r : router.getRoutingTable().getAllRoutes())
            {
                if(r.getType() == RouteType.STATIC)
                {
                    extLinks.add(new LinkAdvertisement(r.getNetworkAddress(), r.getSubnetMask(), selfId, Math.max(1, r.getMetric())));
                }
            }
            if(!extLinks.isEmpty())
            {
                for(Integer areaId : getActiveAreas())
                {
                    OspfPacket ext = OspfPacket.createLsa(LsaType.EXTERNAL, selfId, selfId, areaId, sequenceNumber, extLinks);
                    areaLsdb.computeIfAbsent(areaId, k -> new LinkedHashMap<>()).put(ext.getLsdbKey(), ext);
                    floodArea(router, areaId, ext);
                }
            }
        }
    }

    private void recalculateSpf(Router router)
    {
        String selfId = resolveRouterId(router);
        Set<Integer> areas = getActiveAreas();
        router.getRoutingTable().flushOspfRoutes();

        Map<String, RouteEntry> intraRoutes = new LinkedHashMap<>();
        Map<Integer, Map<String, RouteEntry>> nextHopToRouter = new HashMap<>();

        for(Integer areaId : areas)
        {
            Map<String, RouteEntry> rHops = new HashMap<>();
            nextHopToRouter.put(areaId, rHops);
            runAreaDijkstra(router, selfId, areaId, intraRoutes, rHops);
        }
        for(RouteEntry r : intraRoutes.values()) router.addRoute(r);

        // ABR: resumen entre Area 0 y areas regulares (O IA)
        if(areas.contains(0) && areas.size() > 1)
        {
            for(Integer targetArea : areas)
            {
                List<LinkAdvertisement> summary = new ArrayList<>();
                for(RouteEntry r : router.getRoutingTable().getAllRoutes())
                {
                    if(r.getType() != RouteType.CONNECTED && r.getType() != RouteType.OSPF) continue;
                    InterfaceState outState = activeInterfaces.get(r.getOutInterface().getName().toLowerCase());
                    if(outState == null || outState.areaId == targetArea) continue;
                    if(targetArea != 0 && outState.areaId != 0) continue;
                    summary.add(new LinkAdvertisement(r.getNetworkAddress(), r.getSubnetMask(), selfId, Math.max(1, r.getMetric())));
                }
                if(!summary.isEmpty())
                {
                    OspfPacket sumLsa = OspfPacket.createLsa(LsaType.SUMMARY, selfId, selfId, targetArea, ++sequenceNumber, summary);
                    Map<String, OspfPacket> db = areaLsdb.computeIfAbsent(targetArea, k -> new LinkedHashMap<>());
                    OspfPacket old = db.get(sumLsa.getLsdbKey());
                    if(old == null || old.getLinks().size() != summary.size())
                    {
                        db.put(sumLsa.getLsdbKey(), sumLsa);
                        floodArea(router, targetArea, sumLsa);
                    }
                }
            }
        }

        // Instalar rutas Inter-Area (O IA) y Externas (O E2) anunciadas por ABR/ASBR
        for(Integer areaId : areas)
        {
            Map<String, OspfPacket> db = areaLsdb.get(areaId);
            Map<String, RouteEntry> rHops = nextHopToRouter.get(areaId);
            if(db == null || rHops == null) continue;

            for(OspfPacket lsa : db.values())
            {
                if(selfId.equals(lsa.getOriginRouterId())) continue;
                RouteEntry hop = rHops.get(lsa.getOriginRouterId());
                if(hop == null) continue;

                for(LinkAdvertisement ad : lsa.getLinks())
                {
                    if(lsa.getLsaType() == LsaType.SUMMARY)
                    {
                        router.addRoute(RouteEntry.createOspfInterAreaRoute(ad.getNetworkAddress(), ad.getSubnetMask(), hop.getNextHopIp(), hop.getOutInterface(), hop.getMetric() + ad.getCost()));
                    }
                    else if(lsa.getLsaType() == LsaType.EXTERNAL)
                    {
                        router.addRoute(RouteEntry.createOspfExternalRoute(ad.getNetworkAddress(), ad.getSubnetMask(), hop.getNextHopIp(), hop.getOutInterface(), ad.getCost()));
                    }
                }
            }
        }
    }

    private void runAreaDijkstra(Router router, String selfId, int areaId, Map<String, RouteEntry> intraRoutes, Map<String, RouteEntry> routerHops)
    {
        Map<String, OspfPacket> db = areaLsdb.get(areaId);
        if(db == null || db.isEmpty()) return;

        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Map<String, String> netMap = new HashMap<>(), maskMap = new HashMap<>(), ifaceIpMap = new HashMap<>();

        for(OspfPacket lsa : db.values())
        {
            if(lsa.getLsaType() != LsaType.ROUTER) continue;
            String rVertex = "R:" + lsa.getOriginRouterId();
            graph.addVertex(rVertex);

            for(LinkAdvertisement ad : lsa.getLinks())
            {
                String net = SubnetUtils.getNetworkAddress(ad.getNetworkAddress(), ad.getSubnetMask());
                String prefix = net + "/" + SubnetUtils.maskToPrefixLength(ad.getSubnetMask());
                String nVertex = "N:" + prefix;

                graph.addVertex(nVertex);
                netMap.put(prefix, net);
                maskMap.put(prefix, ad.getSubnetMask());
                ifaceIpMap.put(lsa.getOriginRouterId() + "@" + prefix, ad.getInterfaceIp());

                DefaultWeightedEdge e1 = graph.addEdge(rVertex, nVertex);
                if(e1 != null) graph.setEdgeWeight(e1, ad.getCost());
                DefaultWeightedEdge e2 = graph.addEdge(nVertex, rVertex);
                if(e2 != null) graph.setEdgeWeight(e2, 0.0);
            }
        }

        String selfVertex = "R:" + selfId;
        if(!graph.containsVertex(selfVertex)) return;
        DijkstraShortestPath<String, DefaultWeightedEdge> spf = new DijkstraShortestPath<>(graph);

        for(String v : graph.vertexSet())
        {
            if(v.equals(selfVertex)) continue;
            GraphPath<String, DefaultWeightedEdge> path = spf.getPath(selfVertex, v);
            if(path == null || path.getVertexList().size() < 3) continue;

            List<String> nodes = path.getVertexList();
            String transitPrefix = nodes.get(1).substring(2);
            String firstHopRid = nodes.get(2).substring(2);

            NIC outNic = findLocalNicForPrefix(router, transitPrefix);
            String nextHopIp = ifaceIpMap.get(firstHopRid + "@" + transitPrefix);
            if(outNic == null || nextHopIp == null) continue;

            int cost = (int) Math.round(path.getWeight());
            if(v.startsWith("R:"))
            {
                routerHops.put(v.substring(2), RouteEntry.createOspfRoute("0.0.0.0", "0.0.0.0", nextHopIp, outNic, cost));
            }
            else if(v.startsWith("N:") && nodes.size() >= 4)
            {
                String targetPrefix = v.substring(2);
                RouteEntry candidate = RouteEntry.createOspfRoute(netMap.get(targetPrefix), maskMap.get(targetPrefix), nextHopIp, outNic, cost);
                RouteEntry prev = intraRoutes.get(targetPrefix);
                if(prev == null || candidate.getMetric() < prev.getMetric()) intraRoutes.put(targetPrefix, candidate);
            }
        }
    }

    private void sendHello(Router router, InterfaceState iface)
    {
        if(iface.nic.getIpAddress() == null) return;
        List<String> heard = new ArrayList<>();
        for(NeighborState n : neighbors.values())
        {
            if(n.localNic.equals(iface.nic) && n.state != NeighborFsm.DOWN) heard.add(n.routerId);
        }
        OspfPacket hello = OspfPacket.createHello(resolveRouterId(router), iface.nic.getIpAddress(), iface.areaId, iface.drId, iface.bdrId, heard);
        transmitOspf(router, iface.nic, ALL_SPF_IP, ALL_SPF_MAC, hello);
    }

    private void floodArea(Router router, int areaId, OspfPacket lsa)
    {
        for(InterfaceState iface : activeInterfaces.values())
        {
            if(iface.areaId == areaId) sendLsa(router, iface, lsa);
        }
    }

    private void sendLsa(Router router, InterfaceState iface, OspfPacket lsa)
    {
        if(iface.nic.getIpAddress() == null) return;
        String selfId = resolveRouterId(router);
        boolean hasDr = !OspfPacket.NONE_ID.equals(iface.drId);
        boolean isDrOrBdr = selfId.equals(iface.drId) || selfId.equals(iface.bdrId);

        if(hasDr && !isDrOrBdr) transmitOspf(router, iface.nic, ALL_DR_IP, ALL_DR_MAC, lsa);
        else transmitOspf(router, iface.nic, ALL_SPF_IP, ALL_SPF_MAC, lsa);
    }

    private void transmitOspf(Router router, NIC outNic, String destIp, String destMac, OspfPacket ospf)
    {
        FrameHandler handler = router.getFrameHandler(EtherType.IPV4);
        if(handler instanceof Ipv4Process)
        {
            ((Ipv4Process) handler).sendMulticastPacket(router, outNic, destMac, new IpPacket(outNic.getIpAddress(), destIp, Protocol.OSPF, ospf.toPayload()));
        }
    }

    private NIC resolveIncomingNic(Router router, String sourceIp)
    {
        for(NIC nic : router.getInterfaces())
        {
            if(nic.getIpAddress() != null && SubnetUtils.matchesSubnet(sourceIp, nic.getIpAddress(), nic.getSubnetMask())) return nic;
        }
        return null;
    }

    private NIC findLocalNicForPrefix(Router router, String prefixKey)
    {
        for(RouteEntry r : router.getRoutingTable().getAllRoutes())
        {
            if(r.getType() == RouteType.CONNECTED && r.getPrefixKey().equals(prefixKey)) return r.getOutInterface();
        }
        return null;
    }

    private int getLinkCost(Router router, NIC nic)
    {
        if(router.getNetwork() != null)
        {
            Link link = router.getNetwork().getLinkOnPort(nic);
            if(link != null && link.getLinkWeight() > 0) return (int) Math.round(link.getLinkWeight());
        }
        return 10;
    }

    private Set<Integer> getActiveAreas()
    {
        Set<Integer> areas = new HashSet<>();
        for(InterfaceState iface : activeInterfaces.values()) areas.add(iface.areaId);
        return areas;
    }

    public String resolveRouterId(Router router)
    {
        if(configuredRouterId != null && !configuredRouterId.isEmpty()) return configuredRouterId;
        String highestIp = null;
        int highestVal = 0;
        for(NIC nic : router.getInterfaces())
        {
            if(nic.getIpAddress() != null && SubnetUtils.isValidIp(nic.getIpAddress()))
            {
                int val = SubnetUtils.ipToInt(nic.getIpAddress());
                if(highestIp == null || Integer.compareUnsigned(val, highestVal) > 0)
                {
                    highestIp = nic.getIpAddress();
                    highestVal = val;
                }
            }
        }
        return highestIp != null ? highestIp : router.getDisplayName();
    }

    public String formatNeighbors(Router router)
    {
        StringBuilder sb = new StringBuilder("\n--- OSPF NEIGHBORS (Process " + processId + " | RID " + resolveRouterId(router) + ") ---\n");
        if(neighbors.isEmpty()) return sb.append("(Sin vecinos OSPF)\n------------------------------------------------------------------").toString();

        sb.append(String.format("%-16s %-14s %-16s %-10s\n", "Neighbor ID", "State", "Address", "Interface"));
        for(NeighborState n : neighbors.values())
        {
            InterfaceState iface = activeInterfaces.get(n.localNic.getName().toLowerCase());
            String role = (iface == null) ? "" : n.routerId.equals(iface.drId) ? "/DR" : n.routerId.equals(iface.bdrId) ? "/BDR" : "/DROTHER";
            sb.append(String.format("%-16s %-14s %-16s %-10s\n", n.routerId, n.state.name() + role, n.neighborIp, n.localNic.getName()));
        }
        return sb.append("------------------------------------------------------------------").toString();
    }

    public String formatDatabase(Router router)
    {
        StringBuilder sb = new StringBuilder("\n--- OSPF LSDB (RID " + resolveRouterId(router) + ") ---\n");
        if(areaLsdb.isEmpty()) return sb.append("(LSDB Vacia)\n---------------------------------------").toString();

        for(Map.Entry<Integer, Map<String, OspfPacket>> area : areaLsdb.entrySet())
        {
            sb.append("Area ").append(area.getKey()).append(":\n");
            for(OspfPacket lsa : area.getValue().values())
            {
                sb.append("  [").append(lsa.getLsaType()).append("] Origin: ").append(lsa.getOriginRouterId()).append(" (Seq ").append(lsa.getSequenceNumber()).append(")\n");
                for(LinkAdvertisement ad : lsa.getLinks())
                {
                    sb.append("     -> ").append(ad.getNetworkAddress()).append("/").append(SubnetUtils.maskToPrefixLength(ad.getSubnetMask())).append(" cost ").append(ad.getCost()).append("\n");
                }
            }
        }
        return sb.append("---------------------------------------").toString();
    }
}