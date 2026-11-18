package com.example.behaviour;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.interfaces.FrameHandler;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.Link;
import com.example.network.devices.CircuitEntry;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.RouteEntry;
import com.example.network.devices.Router;
import com.example.network.devices.SubnetUtils;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.IpPacket;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.packets.Payload;
import com.example.network.processes.CircuitProcess;
import com.example.network.processes.Ipv4Process;
import com.example.network.processes.OspfProcess;
import com.example.network.processes.PingProcess;

public class BasicConsole extends Console
{
    private NetworkNode owner;
    private Map<String, NetworkNode> nodeDirectory;
    private int activeOspfProcessId = 1;

    public void bind(NetworkNode owner, Map<String, NetworkNode> nodeDirectory)
    {
        this.owner = owner;
        this.nodeDirectory = nodeDirectory;
    }

    @Override
    public String process(String user, String rawCommand)
    {
        if(rawCommand == null || rawCommand.trim().isEmpty())
        {
            return "";
        }

        String command = rawCommand.trim();
        this.log(user, command);

        if(owner == null)
        {
            return "Error: Consola no vinculada a ningun nodo.";
        }

        String upper = command.toUpperCase();

        if(upper.equals("HELP") || upper.equals("?")) return buildHelpText();
        if(upper.equals("LOG")) return buildLogDump();
        if(upper.equals("ARP")) return buildArpTable();
        if(upper.equals("ROUTES") || upper.equals("SHOW IP ROUTE") || upper.equals("SH IP ROUTE")) return buildRoutingTable();
        if(upper.equals("INTERFACES") || upper.equals("SHOW IP INTERFACE BRIEF") || upper.equals("SH IP INT BR")) return buildInterfacesBrief();
        if(upper.equals("SHOW IP OSPF NEIGHBOR") || upper.equals("SH IP OSPF NEI")) return handleShowOspfNeighbor();
        if(upper.equals("SHOW IP OSPF DATABASE") || upper.equals("SH IP OSPF DB")) return handleShowOspfDatabase();
        if(upper.startsWith("IP ADDRESS ")) return handleIpAddress(command);
        if(upper.startsWith("IP ROUTE ")) return handleIpRoute(command);
        if(upper.startsWith("ROUTER OSPF") || upper.equals("ENABLE OSPF")) return handleRouterOspf(command);
        if(upper.startsWith("ROUTER-ID ")) return handleRouterId(command);
        if(upper.startsWith("NETWORK ")) return handleNetworkArea(command);
        if(upper.equals("REDISTRIBUTE STATIC")) return handleRedistributeStatic(true);
        if(upper.equals("NO REDISTRIBUTE STATIC")) return handleRedistributeStatic(false);
        if(upper.startsWith("PING ")) return handlePing(command);
        if(upper.startsWith("SEND_P ")) return handleSendPacket(command);
        if(upper.startsWith("CONNECT ")) return handleCircuitConnect(command);
        if(upper.startsWith("SEND_C ")) return handleCircuitSend(command);
        if(upper.equals("DISCONNECT")) return handleCircuitDisconnect();

        return "Comando no reconocido. Escribe HELP para ver la lista de comandos.";
    }

    private String handleIpAddress(String command)
    {
        String[] tokens = command.split("\\s+");
        if(tokens.length != 5) return "Uso: ip address <interfaz> <ip> <mascara>";

        String nicName = tokens[2];
        String ip = tokens[3];
        String mask = tokens[4];

        if(!SubnetUtils.isValidIp(ip) || !SubnetUtils.isValidIp(mask))
        {
            return "Error: Direccion IP o mascara invalida.";
        }

        NIC nic = owner.getInterfaceByName(nicName);
        if(nic == null)
        {
            return "Error: Interfaz '" + nicName + "' no encontrada en " + owner.getDisplayName() + ".";
        }

        owner.queueInstruction(ctx -> nic.setIpConfiguration(ip, mask));
        return "Interfaz " + nic.getName() + " configurada con " + ip + " " + mask + " (Ruta Conectada C generada).";
    }

    private String handleIpRoute(String command)
    {
        String[] tokens = command.split("\\s+");
        if(tokens.length < 5 || tokens.length > 6)
        {
            return "Uso: ip route <red> <mascara> <next-hop-ip> [interfaz]";
        }

        String network = tokens[2];
        String mask = tokens[3];
        String nextHop = tokens[4];

        if(!SubnetUtils.isValidIp(network) || !SubnetUtils.isValidIp(mask) || !SubnetUtils.isValidIp(nextHop))
        {
            return "Error: Parametros IP invalidos en ip route.";
        }

        NIC outNic = null;
        if(tokens.length == 6)
        {
            outNic = owner.getInterfaceByName(tokens[5]);
            if(outNic == null) return "Error: Interfaz '" + tokens[5] + "' no existe.";
        }
        else
        {
            RouteEntry via = owner.resolveRoute(nextHop);
            if(via != null) outNic = via.getOutInterface();
        }

        if(outNic == null)
        {
            return "Error: No se pudo determinar interfaz de salida hacia " + nextHop + ". Especifica la interfaz.";
        }

        final NIC resolvedNic = outNic;
        owner.queueInstruction(ctx -> {
            ctx.addRoute(RouteEntry.createStaticRoute(network, mask, nextHop, resolvedNic, 1));
            if(ctx instanceof Router && ((Router) ctx).isOspfEnabled())
            {
                ((Router) ctx).getOrCreateOspfProcess().onTopologyChanged((Router) ctx);
            }
        });

        return "Ruta estatica S " + SubnetUtils.getNetworkAddress(network, mask) + "/" + SubnetUtils.maskToPrefixLength(mask)
            + " via " + nextHop + " (" + outNic.getName() + ") agregada.";
    }

    private String handleRouterOspf(String command)
    {
        if(!(owner instanceof Router)) return "Error: OSPF solo esta disponible en dispositivos Router.";

        String[] tokens = command.split("\\s+");
        int pid = 1;
        if(tokens.length >= 3 && tokens[0].equalsIgnoreCase("router"))
        {
            try { pid = Integer.parseInt(tokens[2]); }
            catch(NumberFormatException e) { return "Error: Process ID invalido."; }
        }

        this.activeOspfProcessId = pid;
        final int finalPid = pid;
        owner.queueInstruction(ctx -> ((Router) ctx).enableOspf(finalPid));
        return "Proceso OSPF 1.jg (PID " + pid + ") habilitado en " + owner.getDisplayName() + ".";
    }

    private String handleRouterId(String command)
    {
        if(!(owner instanceof Router)) return "Error: router-id solo aplica a dispositivos Router.";
        String[] tokens = command.split("\\s+");
        if(tokens.length != 2 || !SubnetUtils.isValidIp(tokens[1])) return "Uso: router-id <A.B.C.D>";

        String rid = tokens[1];
        owner.queueInstruction(ctx -> ((Router) ctx).getOrCreateOspfProcess().setRouterId((Router) ctx, rid));
        return "OSPF Router-ID configurado en " + rid + ".";
    }

    private String handleNetworkArea(String command)
    {
        if(!(owner instanceof Router)) return "Error: El comando network ... area solo aplica a dispositivos Router.";

        String[] tokens = command.split("\\s+");
        if(tokens.length != 5 || !tokens[3].equalsIgnoreCase("area"))
        {
            return "Uso: network <red> <mascara|wildcard> area <area-id>";
        }

        String net = tokens[1];
        String mask = tokens[2];
        int areaId;
        try { areaId = Integer.parseInt(tokens[4]); }
        catch(NumberFormatException e) { return "Error: Area ID invalido."; }

        if(!SubnetUtils.isValidIp(net) || !SubnetUtils.isValidIp(mask))
        {
            return "Error: Red o mascara/wildcard invalida.";
        }

        final int pid = activeOspfProcessId;
        owner.queueInstruction(ctx -> {
            Router r = (Router) ctx;
            if(!r.isOspfEnabled()) r.enableOspf(pid);
            r.getOrCreateOspfProcess().addNetworkStatement(r, net, mask, areaId);
        });

        return "Red " + net + " (" + mask + ") asignada al Area " + areaId + " en OSPF.";
    }

    private String handleRedistributeStatic(boolean enable)
    {
        if(!(owner instanceof Router)) return "Error: redistribute static solo aplica a dispositivos Router.";
        owner.queueInstruction(ctx -> ((Router) ctx).getOrCreateOspfProcess().setRedistributeStatic((Router) ctx, enable));
        return enable
            ? "Redistribucion de rutas estaticas (ASBR / LSA External) activada."
            : "Redistribucion de rutas estaticas desactivada.";
    }

    private String handleShowOspfNeighbor()
    {
        if(!(owner instanceof Router)) return "Error: Comando exclusivo de Router.";
        Router r = (Router) owner;
        if(!r.isOspfEnabled()) return "OSPF no esta habilitado en " + r.getDisplayName() + ".";
        return r.getOrCreateOspfProcess().formatNeighbors(r);
    }

    private String handleShowOspfDatabase()
    {
        if(!(owner instanceof Router)) return "Error: Comando exclusivo de Router.";
        Router r = (Router) owner;
        if(!r.isOspfEnabled()) return "OSPF no esta habilitado en " + r.getDisplayName() + ".";
        return r.getOrCreateOspfProcess().formatDatabase(r);
    }

    private String handlePing(String command)
    {
        String[] tokens = command.split("\\s+");
        if(tokens.length != 2) return "Uso: ping <host|IP>";

        String targetIp = resolveTargetIp(tokens[1]);
        if(targetIp == null) return "Error: No se pudo resolver el host '" + tokens[1] + "'.";

        owner.queueInstruction(ctx -> {
            IpProtocolHandler handler = ctx.getIpHandler(Protocol.PING);
            if(handler instanceof PingProcess) ((PingProcess) handler).sendPing(ctx, targetIp);
        });
        return "Enviando ICMP Echo Request hacia " + targetIp + "... (revisa LOG para ver respuesta)";
    }

    private String handleSendPacket(String command)
    {
        Matcher m = Pattern.compile("^SEND_P\\s+(\\S+)\\s+\"([^\"]*)\"$", Pattern.CASE_INSENSITIVE).matcher(command);
        if(!m.matches()) return "Uso: SEND_P <host|IP> \"mensaje\"";

        String targetIp = resolveTargetIp(m.group(1));
        String message = m.group(2);
        if(targetIp == null) return "Error: Destino '" + m.group(1) + "' no encontrado.";

        owner.queueInstruction(ctx -> {
            RouteEntry route = ctx.resolveRoute(targetIp);
            if(route == null || route.getOutInterface() == null)
            {
                this.log("Red", "Error SEND_P: Sin ruta hacia " + targetIp);
                return;
            }
            FrameHandler fh = ctx.getFrameHandler(EtherType.IPV4);
            if(fh instanceof Ipv4Process)
            {
                IpPacket pkt = new IpPacket(route.getOutInterface().getIpAddress(), targetIp, Protocol.DATA, new Payload(message));
                ((Ipv4Process) fh).sendIpPacket(ctx, pkt);
            }
        });
        return "Paquete IPv4 encolado hacia " + targetIp + ".";
    }

    private String handleCircuitConnect(String command)
    {
        String[] tokens = command.split("\\s+");
        if(tokens.length != 2) return "Uso: CONNECT <host|IP>";

        String targetIp = resolveTargetIp(tokens[1]);
        if(targetIp == null) return "Error: Destino '" + tokens[1] + "' no encontrado.";

        UUID circuitId = UUID.randomUUID();
        owner.queueInstruction(ctx -> {
            FrameHandler fh = ctx.getFrameHandler(EtherType.CIRCUIT);
            if(fh instanceof CircuitProcess) ((CircuitProcess) fh).initiateSetup(ctx, circuitId, targetIp);
        });
        return "Solicitando circuito hacia " + targetIp + " (ID: " + circuitId.toString().substring(0, 8) + ")...";
    }

    private String handleCircuitSend(String command)
    {
        Matcher m = Pattern.compile("^SEND_C\\s+\"([^\"]*)\"$", Pattern.CASE_INSENSITIVE).matcher(command);
        if(!m.matches()) return "Uso: SEND_C \"mensaje\"";

        String message = m.group(1);
        UUID activeCircuit = findOutgoingCircuitId();
        if(activeCircuit == null) return "Error: No hay ningun circuito activo establecido. Usa CONNECT <destino> primero.";

        owner.queueInstruction(ctx -> {
            FrameHandler fh = ctx.getFrameHandler(EtherType.CIRCUIT);
            if(fh instanceof CircuitProcess) ((CircuitProcess) fh).sendData(ctx, activeCircuit, "0.0.0.0", new Payload(message));
        });
        return "Datos enviados por circuito " + activeCircuit.toString().substring(0, 8) + ".";
    }

    private String handleCircuitDisconnect()
    {
        UUID activeCircuit = findOutgoingCircuitId();
        if(activeCircuit == null) return "No hay circuito activo para cerrar.";

        owner.queueInstruction(ctx -> {
            FrameHandler fh = ctx.getFrameHandler(EtherType.CIRCUIT);
            if(fh instanceof CircuitProcess) ((CircuitProcess) fh).initiateTeardown(ctx, activeCircuit);
        });
        return "Cerrando circuito " + activeCircuit.toString().substring(0, 8) + "...";
    }

    private UUID findOutgoingCircuitId()
    {
        for(Map.Entry<UUID, CircuitEntry> entry : owner.getCircuitTable().entrySet())
        {
            if(entry.getValue().getInNic() == null) return entry.getKey();
        }
        return null;
    }

    private String resolveTargetIp(String hostOrIp)
    {
        if(SubnetUtils.isValidIp(hostOrIp)) return hostOrIp;
        if(nodeDirectory != null)
        {
            for(NetworkNode node : nodeDirectory.values())
            {
                if(node.getDisplayName().equalsIgnoreCase(hostOrIp))
                {
                    for(NIC nic : node.getInterfaces())
                    {
                        if(nic.getIpAddress() != null && !nic.getName().toLowerCase().startsWith("lo"))
                        {
                            return nic.getIpAddress();
                        }
                    }
                    for(NIC nic : node.getInterfaces())
                    {
                        if(nic.getIpAddress() != null) return nic.getIpAddress();
                    }
                }
            }
        }
        return null;
    }

    private String buildRoutingTable()
    {
        StringBuilder sb = new StringBuilder("\n--- TABLA DE ENRUTAMIENTO (" + owner.getDisplayName() + ") ---\n");
        sb.append("Codes: C - connected, S - static, O - OSPF, O IA - OSPF inter area, O E2 - OSPF external\n\n");

        if(owner.getRoutingTable().isEmpty())
        {
            sb.append("(Tabla vacia)\n");
        }
        else
        {
            for(RouteEntry r : owner.getRoutingTable().getAllRoutes())
            {
                sb.append(String.format("%-5s %-18s ", r.getType().getCode(), r.getPrefixKey()));
                if(r.isDirectRoute())
                {
                    sb.append("is directly connected, ").append(r.getOutInterface().getName());
                }
                else
                {
                    sb.append("[metric ").append(r.getMetric()).append("] via ").append(r.getNextHopIp())
                      .append(", ").append(r.getOutInterface().getName());
                }
                sb.append("\n");
            }
        }
        sb.append("--------------------------------------------------");
        return sb.toString();
    }

    private String buildInterfacesBrief()
    {
        StringBuilder sb = new StringBuilder("\n--- INTERFACES DE " + owner.getDisplayName() + " ---\n");
        sb.append(String.format("%-10s %-16s %-16s %-14s %-8s\n", "Interface", "IP-Address", "Subnet-Mask", "MAC", "Status"));
        for(NIC nic : owner.getInterfaces())
        {
            String status = "DOWN";
            if(owner.getNetwork() != null)
            {
                Link link = owner.getNetwork().getLinkOnPort(nic);
                if(link != null && link.isPhysicallyUp()) status = "UP";
            }
            if(nic.getName().toLowerCase().startsWith("lo") && nic.getIpAddress() != null) status = "UP";

            sb.append(String.format("%-10s %-16s %-16s %-14s %-8s\n",
                nic.getName(),
                nic.getIpAddress() != null ? nic.getIpAddress() : "unassigned",
                nic.getSubnetMask() != null ? nic.getSubnetMask() : "-",
                nic.getMacAddress(),
                status
            ));
        }
        sb.append("------------------------------------------------------------------");
        return sb.toString();
    }

    private String buildArpTable()
    {
        StringBuilder sb = new StringBuilder("\n--- TABLA ARP (" + owner.getDisplayName() + ") ---\n");
        if(owner.getArpTable().isEmpty())
        {
            sb.append("(Tabla ARP vacia)\n");
        }
        else
        {
            for(Map.Entry<String, String> entry : owner.getArpTable().entrySet())
            {
                sb.append(String.format("%-16s -> %s\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("-----------------------------------");
        return sb.toString();
    }

    private String buildLogDump()
    {
        StringBuilder sb = new StringBuilder("\n--- HISTORIAL DE CONSOLA (" + owner.getDisplayName() + ") ---\n");
        for(String line : consoleLog) sb.append(line).append("\n");
        sb.append("-----------------------------------------------");
        return sb.toString();
    }

    private String buildHelpText()
    {
        return "\nComandos disponibles:\n"
            + "  ip address <nic> <ip> <mask>               - Configura IP/mascara y ruta conectada (C)\n"
            + "  ip route <red> <mask> <nextHop> [nic]      - Agrega ruta estatica (S)\n"
            + "  router ospf [pid]                          - Habilita OSPF 1.jg (Solo Router)\n"
            + "  router-id <A.B.C.D>                        - Define Router-ID OSPF (Solo Router)\n"
            + "  network <red> <mask|wildcard> area <id>    - Anuncia subred en Area OSPF (Solo Router)\n"
            + "  redistribute static                        - Exporta rutas estaticas como O E2 (Solo Router)\n"
            + "  show ip route | ROUTES                     - Muestra tabla de enrutamiento\n"
            + "  show ip interface brief                    - Muestra estado de interfaces\n"
            + "  show ip ospf neighbor                      - Muestra vecinos y roles DR/BDR\n"
            + "  show ip ospf database                      - Muestra LSDB por area\n"
            + "  ping <host|IP>                             - Envia ICMP Echo Request\n"
            + "  SEND_P <host|IP> \"msg\"                     - Envia mensaje IPv4\n"
            + "  CONNECT <host|IP> | SEND_C \"msg\" | DISCONNECT - Conmutacion de circuitos\n"
            + "  ARP | LOG | EXIT_CONSOLE";
    }
}