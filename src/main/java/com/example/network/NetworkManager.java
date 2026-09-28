package com.example.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import com.example.interfaces.ManagerCommand;

public class NetworkManager implements Runnable
{
    public JGraphTNetwork network = new JGraphTNetwork();
    private Map<UUID, List<Link>> activeConnections = new HashMap<>();
    private volatile boolean active = true;
    
    private LinkedBlockingQueue<ManagerCommand> listener = new LinkedBlockingQueue<>();

    public NetworkManager() {}

    public void run()
    {
        while(active)
        {
            try 
            {    
                ManagerCommand cmd = listener.take();
                cmd.execute(this);
            } 
            catch(InterruptedException e) 
            {
                System.out.println("NetworkManager interrumpido.");
                Thread.currentThread().interrupt();
                break;
            } 
            catch(Exception e) 
            {
                System.out.println("Error en NetworkManager: " + e.getMessage());
            }
        }
    }

    public void queueCommand(ManagerCommand cmd) 
    {
        try 
        {
            listener.put(cmd);
        } 
        catch(InterruptedException e) 
        {
            Thread.currentThread().interrupt();
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

    public void eraseConn(UUID token)
    {
        activeConnections.remove(token);
    }   
}