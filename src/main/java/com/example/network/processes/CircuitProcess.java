package com.example.network.processes;

import java.util.UUID;
import com.example.interfaces.FrameHandler;
import com.example.network.JGraphTNetwork;
import com.example.network.Link;
import com.example.network.devices.CircuitEntry;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.Pc;
import com.example.network.devices.RouteEntry;
import com.example.network.packets.CircuitPacket;
import com.example.network.packets.CircuitPacket.ControlType;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.Payload;

public class CircuitProcess implements FrameHandler
{
    public void initiateSetup(NetworkNode origin, UUID circuitId, String destinationIp)
    {
        JGraphTNetwork network = origin.getNetwork();
        if(network == null)
        {
            return;
        }

        RouteEntry route = origin.resolveRoute(destinationIp);
        if(route == null)
        {
            notifyOrigin(origin, "No existe ruta hacia " + destinationIp + " para establecer circuito.");
            return;
        }

        NIC outNic = route.getOutInterface();
        Link outLink = network.getLinkOnPort(outNic);
        if(outLink == null || !outLink.isAvailable())
        {
            notifyOrigin(origin, "Enlace ocupado o no disponible en " + outNic.getName() + ".");
            return;
        }

        outLink.reserveCircuit(circuitId);
        origin.getCircuitTable().put(circuitId, new CircuitEntry(null, outNic));

        CircuitPacket setup = new CircuitPacket(circuitId, ControlType.SETUP, outNic.getIpAddress(), destinationIp, null);
        sendCircuitSignal(origin, outNic, setup, circuitId);
    }

    public void sendData(NetworkNode origin, UUID circuitId, String destinationIp, Payload payload)
    {
        CircuitEntry entry = origin.getCircuitTable().get(circuitId);
        if(entry == null || entry.getOutNic() == null)
        {
            notifyOrigin(origin, "Circuito no valido o inactivo.");
            return;
        }

        NIC outNic = entry.getOutNic();
        CircuitPacket data = new CircuitPacket(circuitId, ControlType.DATA, outNic.getIpAddress(), destinationIp, payload);
        sendCircuitSignal(origin, outNic, data, circuitId);
    }

    public void initiateTeardown(NetworkNode origin, UUID circuitId)
    {
        CircuitEntry entry = origin.getCircuitTable().remove(circuitId);
        if(entry == null || entry.getOutNic() == null || origin.getNetwork() == null)
        {
            return;
        }

        NIC outNic = entry.getOutNic();
        CircuitPacket teardown = new CircuitPacket(circuitId, ControlType.TEARDOWN, outNic.getIpAddress(), null, null);
        sendCircuitSignal(origin, outNic, teardown, circuitId);

        Link outLink = origin.getNetwork().getLinkOnPort(outNic);
        if(outLink != null && outLink.isReservedFor(circuitId))
        {
            outLink.releaseCircuit();
        }

        if(origin instanceof Pc)
        {
            ((Pc) origin).setConnToken(null);
        }
        notifyOrigin(origin, "Circuito " + circuitId.toString().substring(0, 8) + " cerrado.");
    }

    @Override
    public void handleFrame(NetworkNode context, NIC incomingNic, EthernetFrame frame)
    {
        if(!(frame.getPayload() instanceof CircuitPacket))
        {
            return;
        }

        CircuitPacket packet = (CircuitPacket) frame.getPayload();
        UUID circuitId = packet.getCircuitId();

        switch(packet.getControlType())
        {
            case SETUP:
                handleSetup(context, incomingNic, packet);
                break;
            case SETUP_ACK:
                handleSetupAck(context, packet);
                break;
            case SETUP_FAIL:
                handleSetupFail(context, packet);
                break;
            case DATA:
                handleData(context, packet);
                break;
            case TEARDOWN:
                handleTeardown(context, packet);
                break;
        }
    }

    private void handleSetup(NetworkNode context, NIC incomingNic, CircuitPacket packet)
    {
        UUID circuitId = packet.getCircuitId();
        if(context.ownsIp(packet.getDestIp()))
        {
            context.getCircuitTable().put(circuitId, new CircuitEntry(incomingNic, null));
            CircuitPacket ack = new CircuitPacket(circuitId, ControlType.SETUP_ACK, packet.getDestIp(), packet.getSourceIp(), null);
            sendCircuitSignal(context, incomingNic, ack, circuitId);
            notifyOrigin(context, "Circuito entrante aceptado desde " + packet.getSourceIp() + ".");
            return;
        }

        if(!context.isIpForwardingEnabled() || context.getNetwork() == null)
        {
            CircuitPacket fail = new CircuitPacket(circuitId, ControlType.SETUP_FAIL, incomingNic.getIpAddress(), packet.getSourceIp(), null);
            sendCircuitSignal(context, incomingNic, fail, circuitId);
            return;
        }

        RouteEntry route = context.resolveRoute(packet.getDestIp());
        if(route == null)
        {
            CircuitPacket fail = new CircuitPacket(circuitId, ControlType.SETUP_FAIL, incomingNic.getIpAddress(), packet.getSourceIp(), null);
            sendCircuitSignal(context, incomingNic, fail, circuitId);
            return;
        }

        NIC outNic = route.getOutInterface();
        Link outLink = context.getNetwork().getLinkOnPort(outNic);
        if(outLink == null || !outLink.isAvailable())
        {
            CircuitPacket fail = new CircuitPacket(circuitId, ControlType.SETUP_FAIL, incomingNic.getIpAddress(), packet.getSourceIp(), null);
            sendCircuitSignal(context, incomingNic, fail, circuitId);
            return;
        }

        outLink.reserveCircuit(circuitId);
        context.getCircuitTable().put(circuitId, new CircuitEntry(incomingNic, outNic));
        sendCircuitSignal(context, outNic, packet, circuitId);
    }

    private void handleSetupAck(NetworkNode context, CircuitPacket packet)
    {
        UUID circuitId = packet.getCircuitId();
        CircuitEntry entry = context.getCircuitTable().get(circuitId);
        if(entry == null)
        {
            return;
        }

        if(entry.getInNic() == null)
        {
            if(context instanceof Pc)
            {
                ((Pc) context).setConnToken(circuitId);
            }
            notifyOrigin(context, "Circuito establecido con " + packet.getSourceIp() + " (ID: " + circuitId.toString().substring(0, 8) + ").");
        }
        else
        {
            sendCircuitSignal(context, entry.getInNic(), packet, circuitId);
        }
    }

    private void handleSetupFail(NetworkNode context, CircuitPacket packet)
    {
        UUID circuitId = packet.getCircuitId();
        CircuitEntry entry = context.getCircuitTable().remove(circuitId);
        if(entry == null)
        {
            return;
        }

        if(entry.getOutNic() != null && context.getNetwork() != null)
        {
            Link reserved = context.getNetwork().getLinkOnPort(entry.getOutNic());
            if(reserved != null && reserved.isReservedFor(circuitId))
            {
                reserved.releaseCircuit();
            }
        }

        if(entry.getInNic() == null)
        {
            if(context instanceof Pc)
            {
                ((Pc) context).setConnToken(null);
            }
            notifyOrigin(context, "Fallo al establecer circuito: enlace intermedio ocupado o destino inalcanzable.");
        }
        else
        {
            sendCircuitSignal(context, entry.getInNic(), packet, circuitId);
        }
    }

    private void handleData(NetworkNode context, CircuitPacket packet)
    {
        UUID circuitId = packet.getCircuitId();
        CircuitEntry entry = context.getCircuitTable().get(circuitId);
        if(entry == null)
        {
            return;
        }

        if(entry.getOutNic() == null)
        {
            String msg = "[Circuito " + circuitId.toString().substring(0, 8) + "] Mensaje recibido de " + packet.getSourceIp() + ": " + packet.getPayload().asString();
            notifyOrigin(context, msg);
        }
        else
        {
            sendCircuitSignal(context, entry.getOutNic(), packet, circuitId);
        }
    }

    private void handleTeardown(NetworkNode context, CircuitPacket packet)
    {
        UUID circuitId = packet.getCircuitId();
        CircuitEntry entry = context.getCircuitTable().remove(circuitId);
        if(entry == null)
        {
            return;
        }

        if(entry.getOutNic() != null && context.getNetwork() != null)
        {
            sendCircuitSignal(context, entry.getOutNic(), packet, circuitId);
            Link reserved = context.getNetwork().getLinkOnPort(entry.getOutNic());
            if(reserved != null && reserved.isReservedFor(circuitId))
            {
                reserved.releaseCircuit();
            }
        }
        else
        {
            notifyOrigin(context, "Circuito " + circuitId.toString().substring(0, 8) + " cerrado por el extremo remoto.");
        }
    }

    private void sendCircuitSignal(NetworkNode context, NIC outNic, CircuitPacket packet, UUID circuitId)
    {
        JGraphTNetwork network = context.getNetwork();
        if(network == null || outNic == null)
        {
            return;
        }

        Link link = network.getLinkOnPort(outNic);
        if(link == null)
        {
            return;
        }

        NIC remoteNic = link.getOtherPort(outNic);
        String destMac = (remoteNic != null) ? remoteNic.getMacAddress() : EthernetFrame.BROADCAST_MAC;
        EthernetFrame frame = new EthernetFrame(outNic.getMacAddress(), destMac, packet);
        network.transmitCircuitFrame(outNic, frame, circuitId);
    }

    private void notifyOrigin(NetworkNode context, String message)
    {
        if(context instanceof Pc)
        {
            ((Pc) context).console.log("Red", message);
        }
        System.out.println("[" + context.getDisplayName() + "] " + message);
    }
}