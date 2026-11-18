package com.example.network.packets;

import java.util.UUID;

public class CircuitPacket implements FramePayload
{
    public enum ControlType
    {
        SETUP,
        SETUP_ACK,
        SETUP_FAIL,
        DATA,
        TEARDOWN
    }

    private final UUID circuitId;
    private final ControlType controlType;
    private final String sourceIp;
    private final String destIp;
    private final Payload payload;

    public CircuitPacket(UUID circuitId, ControlType controlType, String sourceIp, String destIp, Payload payload)
    {
        this.circuitId = circuitId;
        this.controlType = controlType;
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.payload = payload;
    }

    @Override
    public EtherType getEtherType()
    {
        return EtherType.CIRCUIT;
    }

    public UUID getCircuitId()
    {
        return circuitId;
    }

    public ControlType getControlType()
    {
        return controlType;
    }

    public String getSourceIp()
    {
        return sourceIp;
    }

    public String getDestIp()
    {
        return destIp;
    }

    public Payload getPayload()
    {
        return payload;
    }
}