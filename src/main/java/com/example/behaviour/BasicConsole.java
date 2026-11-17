package com.example.behaviour;

import java.util.HashMap;
import java.util.Map;
import com.example.behaviour.Commands.CircuitSwitching;
import com.example.behaviour.Commands.PacketSwitching;
import com.example.interfaces.SwitchingStrategy;
import com.example.network.devices.Pc;
import com.example.network.devices.RouteEntry;
import com.example.network.packets.Payload;

public class BasicConsole extends Console
{
    private Pc owner;
    private Map<String, String> knownHosts = new HashMap<>();
    private final SwitchingStrategy packetStrategy = new PacketSwitching(5);
    private final CircuitSwitching circuitStrategy = new CircuitSwitching();

    public BasicConsole()
    {
    }

    public void setup(Pc owner, Map<String, String> knownHosts)
    {
        this.owner = owner;
        this.knownHosts = (knownHosts != null) ? knownHosts : new HashMap<>();
    }

    private String resolveTargetIp(String target)
    {
        if(target == null)
        {
            return null;
        }
        if(knownHosts.containsKey(target))
        {
            return knownHosts.get(target);
        }
        for(Map.Entry<String, String> entry : knownHosts.entrySet())
        {
            if(entry.getKey().equalsIgnoreCase(target))
            {
                return entry.getValue();
            }
        }
        if(target.contains("."))
        {
            return target;
        }
        return null;
    }

    private String extractMessage(String command)
    {
        int firstQuote = command.indexOf("\"");
        int lastQuote = command.lastIndexOf("\"");

        if(firstQuote != -1 && lastQuote != -1 && firstQuote != lastQuote)
        {
            return command.substring(firstQuote + 1, lastQuote);
        }
        return null;
    }

    @Override
    public String process(String user, String command)
    {
        this.log(user, command);

        if(owner == null)
        {
            String err = "Error: Consola no configurada. Falta el nodo propietario.";
            this.log("System", err);
            return err;
        }

        String[] parts = command.trim().split(" ", 3);
        String action = parts[0].toUpperCase();
        String response;

        try
        {
            switch(action)
            {
                case "CONNECT":
                    if(parts.length < 2)
                    {
                        response = "Falta nodo destino. Uso: CONNECT [Nodo|IP]";
                        break;
                    }
                    String destConnIp = resolveTargetIp(parts[1]);
                    if(destConnIp != null)
                    {
                        circuitStrategy.connect(owner, destConnIp);
                        response = "Peticion de circuito hacia " + destConnIp + " encolada.";
                    }
                    else
                    {
                        response = "Host desconocido.";
                    }
                    break;

                case "SEND_C":
                    if(owner.getConnToken() != null)
                    {
                        String msg = extractMessage(command);
                        if(msg != null)
                        {
                            circuitStrategy.send(owner, null, new Payload(msg));
                            response = "Mensaje encolado por el circuito activo.";
                        }
                        else
                        {
                            response = "Error de sintaxis. Uso: SEND_C \"mensaje\"";
                        }
                    }
                    else
                    {
                        response = "Error: No tienes ningun circuito establecido.";
                    }
                    break;

                case "DISCONNECT":
                    if(owner.getConnToken() != null)
                    {
                        circuitStrategy.disconnect(owner);
                        response = "Peticion de cierre de circuito encolada.";
                    }
                    else
                    {
                        response = "Error: No hay circuito que cerrar.";
                    }
                    break;

                case "SEND_P":
                    if(parts.length < 2)
                    {
                        response = "Uso: SEND_P [Destino|IP] \"Mensaje\"";
                        break;
                    }
                    String destPacktIp = resolveTargetIp(parts[1]);
                    if(destPacktIp != null)
                    {
                        String msg = extractMessage(command);
                        if(msg != null)
                        {
                            packetStrategy.send(owner, destPacktIp, new Payload(msg));
                            response = "Mensaje fragmentado y encolado por paquetes.";
                        }
                        else
                        {
                            response = "Error de sintaxis. Uso: SEND_P [Destino|IP] \"mensaje\"";
                        }
                    }
                    else
                    {
                        response = "Host desconocido.";
                    }
                    break;

                case "ARP":
                    StringBuilder arpSb = new StringBuilder("\n--- TABLA ARP (" + owner.getDisplayName() + ") ---\n");
                    if(owner.getArpTable().isEmpty())
                    {
                        arpSb.append("(Vacia)\n");
                    }
                    else
                    {
                        for(Map.Entry<String, String> entry : owner.getArpTable().entrySet())
                        {
                            arpSb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
                        }
                    }
                    arpSb.append("-------------------------------");
                    response = arpSb.toString();
                    break;

                case "ROUTES":
                    StringBuilder routeSb = new StringBuilder("\n--- TABLA DE RUTEO (" + owner.getDisplayName() + ") ---\n");
                    for(RouteEntry r : owner.getRoutingTable().values())
                    {
                        String hop = r.isDirectRoute() ? "Directa" : r.getNextHopIp();
                        routeSb.append(r.getDestinationIp()).append(" via ").append(hop)
                               .append(" (").append(r.getOutInterface().getName()).append(")\n");
                    }
                    routeSb.append("------------------------------------");
                    response = routeSb.toString();
                    break;

                case "LOG":
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n--- HISTORIAL DE CONSOLA ---\n");
                    for(String entry : this.consoleLog)
                    {
                        sb.append(entry).append("\n");
                    }
                    sb.append("----------------------------");
                    response = sb.toString();
                    break;

                default:
                    response = "Comandos: CONNECT [nodo], SEND_C \"msg\", DISCONNECT, SEND_P [nodo] \"msg\", ARP, ROUTES, LOG";
            }
        }
        catch(Exception e)
        {
            response = "Fallo interno en consola: " + e.getMessage();
        }

        this.log("System", response);
        return response;
    }
}