package com.example.network.devices;

import com.example.behaviour.BasicConsole;
import com.example.interfaces.IpProtocolHandler;
import com.example.network.packets.IpPacket.Protocol;
import com.example.network.processes.OspfProcess;

public class Router extends NetworkNode
{
    public BasicConsole console = new BasicConsole();
    private boolean ospfEnabled = false;

    public Router(String displayName)
    {
        super(displayName);
    }

    public OspfProcess getOrCreateOspfProcess()
    {
        IpProtocolHandler handler = getIpHandler(Protocol.OSPF);
        if(!(handler instanceof OspfProcess))
        {
            OspfProcess process = new OspfProcess();
            registerIpHandler(Protocol.OSPF, process);
            return process;
        }
        return (OspfProcess) handler;
    }

    public void enableOspf(int processId)
    {
        this.ospfEnabled = true;
        getOrCreateOspfProcess().start(this, processId);
    }

    public boolean isOspfEnabled()
    {
        return ospfEnabled;
    }

    @Override
    public void onInterfaceIpConfigured(NIC nic)
    {
        super.onInterfaceIpConfigured(nic);
        if(ospfEnabled)
        {
            getOrCreateOspfProcess().onTopologyChanged(this);
        }
    }

    @Override
    public void stopNode()
    {
        IpProtocolHandler handler = getIpHandler(Protocol.OSPF);
        if(handler instanceof OspfProcess)
        {
            ((OspfProcess) handler).stop();
        }
        super.stopNode();
    }

    @Override
    public boolean isIpForwardingEnabled()
    {
        return true;
    }
}