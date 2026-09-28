package com.example.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import com.example.interfaces.Event;

public class NetworkManager implements Runnable
{
    public JGraphTNetwork network = new JGraphTNetwork();
    private Map<UUID, List<Link>> activeConnections = new HashMap<>();
    enum Status{PACKET_SWITCHING, CIRCUIT_SWITCHING};
    private volatile boolean active = true;
    LinkedBlockingQueue<Event> listener = new LinkedBlockingQueue<>();

    public NetworkManager()
    {

    }

    public void run()
    {
        while(active)
        {

        }
    }

    public UUID addConnection(List<Link> conn)
    {
        UUID token = UUID.randomUUID();
        activeConnections.put(token, conn);
        return token;
    }

    public List<Link> getConn(UUID token)
    {
        return activeConnections.get(token);
    }
}
