package com.example.behaviour.Commands;

import java.util.UUID;
import com.example.interfaces.FrameHandler;
import com.example.interfaces.SwitchingStrategy;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.Pc;
import com.example.network.packets.FramePayload.EtherType;
import com.example.network.packets.Payload;
import com.example.network.processes.CircuitProcess;

public class CircuitSwitching implements SwitchingStrategy
{
    public void connect(NetworkNode origin, String destinationIp)
    {
        if(origin == null || destinationIp == null)
        {
            return;
        }

        origin.queueInstruction(context -> {
            FrameHandler handler = context.getFrameHandler(EtherType.CIRCUIT);
            if(!(handler instanceof CircuitProcess))
            {
                System.out.println("[" + context.getDisplayName() + "] Proceso de circuitos no disponible.");
                return;
            }

            if(context instanceof Pc && ((Pc) context).getConnToken() != null)
            {
                ((Pc) context).console.log("Red", "Ya existe un circuito activo. Cierra la conexion antes de abrir otra.");
                return;
            }

            UUID circuitId = UUID.randomUUID();
            ((CircuitProcess) handler).initiateSetup(context, circuitId, destinationIp);
        });
    }

    @Override
    public void send(NetworkNode origin, String destinationIp, Payload payload)
    {
        if(origin == null || payload == null)
        {
            return;
        }

        origin.queueInstruction(context -> {
            FrameHandler handler = context.getFrameHandler(EtherType.CIRCUIT);
            if(!(handler instanceof CircuitProcess))
            {
                return;
            }

            UUID circuitId = resolveActiveCircuit(context);
            if(circuitId == null)
            {
                if(context instanceof Pc)
                {
                    ((Pc) context).console.log("Red", "No hay ningun circuito establecido.");
                }
                return;
            }

            ((CircuitProcess) handler).sendData(context, circuitId, destinationIp, payload);
        });
    }

    public void disconnect(NetworkNode origin)
    {
        if(origin == null)
        {
            return;
        }

        origin.queueInstruction(context -> {
            FrameHandler handler = context.getFrameHandler(EtherType.CIRCUIT);
            if(!(handler instanceof CircuitProcess))
            {
                return;
            }

            UUID circuitId = resolveActiveCircuit(context);
            if(circuitId == null)
            {
                return;
            }

            ((CircuitProcess) handler).initiateTeardown(context, circuitId);
        });
    }

    private UUID resolveActiveCircuit(NetworkNode context)
    {
        if(context instanceof Pc)
        {
            return ((Pc) context).getConnToken();
        }
        if(!context.getCircuitTable().isEmpty())
        {
            return context.getCircuitTable().keySet().iterator().next();
        }
        return null;
    }
}