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

    @Override
    public String process(String user, String command) 
    {
        this.log(user, command);
        
        if (owner == null || manager == null) 
        {
            return "Error: Consola no configurada. Faltan dependencias.";
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
                        String msg = command.substring(action.length()).trim();
                        manager.queueCommand(new SendThroughCircuit(owner, msg, owner.getConnToken()));
                        response = "Mensaje enviado por el circuito activo.";
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
                    if(parts.length < 3) return "Uso: SEND_P [Destino] [Mensaje]";
                    Pc destPackt = knownHosts.get(parts[1]);
                    if(destPackt != null) 
                    {
                        String msg = parts[2];
                        manager.queueCommand(new PacketSwitching(owner, destPackt, msg)); 
                        response = "Mensaje procesado y encolado por paquetes.";
                    } 
                    else 
                    {
                        response = "Host desconocido.";
                    }
                    break;
                    
                default:
                    response = "Comandos: CONNECT [nodo], SEND_C [msg], DISCONNECT, SEND_P [nodo] [msg]";
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