package com.example.network.devices;

public class CircuitEntry
{
    private final NIC inNic;
    private final NIC outNic;

    public CircuitEntry(NIC inNic, NIC outNic)
    {
        this.inNic = inNic;
        this.outNic = outNic;
    }

    public NIC getInNic()
    {
        return inNic;
    }

    public NIC getOutNic()
    {
        return outNic;
    }
}