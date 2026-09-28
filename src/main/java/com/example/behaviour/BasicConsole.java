package com.example.behaviour;

import com.example.network.Pc;
import com.example.network.NetworkManager;
import com.example.behaviour.Commands.*;
import java.util.Map;
import java.util.HashMap;

public class BasicConsole extends Console 
{
    private Pc owner;
    private NetworkManager manager;
    private Map<String, Pc> knownHosts = new HashMap<>();

    public BasicConsole() 
    {
    }

    public void setup(Pc owner, NetworkManager manager, Map<String, Pc> knownHosts) 
    {
        this.owner = owner;
        this.manager = manager;
        this.knownHosts = knownHosts;
    }

    private String extractMessage(String command)
    {
        int firstQuote = command.indexOf("\"");
        int lastQuote = command.lastIndexOf("\"");
        
        if (firstQuote != -1 && lastQuote != -1 && firstQuote != lastQuote)
        {
            return command.substring(firstQuote + 1, lastQuote);
        }
        return null;
    }

    @Override
    public String process(String user, String command) 
    {
        this.log(user, command);
        
        if (owner == null || manager == null) 
        {
            String err = "Error: Consola no configurada. Faltan dependencias.";
            this.log("System", err);
            return err;
        }

        String[] parts = command.split(" ", 3);
        String action = parts[0].toUpperCase();
        String response;

        try 
        {
            switch(action) 
            {
                case "CONNECT":
                    if(parts.length < 2) return "Falta nodo destino. Uso: CONNECT [Nodo]";
                    Pc destConn = knownHosts.get(parts[1]);
                    if(destConn != null) 
                    {
                        manager.queueCommand(new CircuitSwitching(owner, destConn));
                        response = "Peticion de circuito hacia " + destConn.getDisplayName() + " encolada.";
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
                            manager.queueCommand(new SendThroughCircuit(owner, msg, owner.getConnToken()));
                            response = "Mensaje enviado por el circuito activo.";
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
                        manager.queueCommand(new TeardownConn(owner, owner.getConnToken()));
                        response = "Peticion de cierre de circuito encolada.";
                    } 
                    else 
                    {
                        response = "Error: No hay circuito que cerrar.";
                    }
                    break;
                    
                case "SEND_P":
                    if(parts.length < 2) return "Uso: SEND_P [Destino] \"Mensaje\"";
                    Pc destPackt = knownHosts.get(parts[1]);
                    if(destPackt != null) 
                    {
                        String msg = extractMessage(command);
                        if(msg != null)
                        {
                            manager.queueCommand(new PacketSwitching(owner, destPackt, msg)); 
                            response = "Mensaje procesado y encolado por paquetes.";
                        }
                        else
                        {
                            response = "Error de sintaxis. Uso: SEND_P [Destino] \"mensaje\"";
                        }
                    } 
                    else 
                    {
                        response = "Host desconocido.";
                    }
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
                    response = "Comandos: CONNECT [nodo], SEND_C \"msg\", DISCONNECT, SEND_P [nodo] \"msg\", LOG";
            }
        } 
        catch (Exception e) 
        {
            response = "Fallo interno en consola: " + e.getMessage();
        }

        this.log("System", response);
        return response;
    }
}