package com.example.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.example.behaviour.BasicConsole;
import com.example.behaviour.Console;
import com.example.interfaces.Event;
import com.example.network.packets.EthernetFrame;
import com.example.network.packets.IpPacket;

public class Pc implements Runnable
{
    String IP, MAC, displayName;
    public Console console = new BasicConsole();
    private volatile boolean active = true;
    LinkedBlockingQueue<Event> listener = new LinkedBlockingQueue<>();
    // El buffer de entrada físico de la tarjeta de red
    public LinkedBlockingQueue<EthernetFrame> rxBuffer = new LinkedBlockingQueue<>();
    // Nuevos tablas de enrutamiento
    private Map<String, String> arpTable = new HashMap<>();
    private Map<String, String> routingTable = new HashMap<>();
    // Capa 1: Interfaces de red
    public List<NetworkInterface> interfaces = new ArrayList<>();
    // 
    public Set<String> listenedIps = new HashSet<>();


    UUID connToken;

    public Pc(String displayName)
    {
      this.displayName = displayName;
      this.MAC = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
      this.status = Status.AVAILABLE;
      this.IP = "0.0.0.0";
      // this.Console = new Console();
    }

    public void setIP(String iP) 
    {
      this.IP = iP;
    }

    public void setDisplayName(String displayName) 
    {
      this.displayName = displayName;
    }

    public String getDisplayName() 
    {
      return displayName;
    }

    public UUID getConnToken() 
    {
      return connToken;
    }

    public void setConnToken(UUID connToken) 
    {
      this.connToken = connToken;
    }

    @Override
    public int hashCode()
     {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((MAC == null) ? 0 : MAC.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) 
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Pc other = (Pc) obj;
      if (MAC == null) 
      {
        if (other.MAC != null)
          return false;
      } 
      else if (!MAC.equals(other.MAC))
        return false;
      return true;
    }

    public void queueInstruction(Event event)
    {
        try
        {
          listener.put(event);
        }
        catch(InterruptedException e)
        {
          Thread.currentThread().interrupt();
        }
    }

    public void run()
    {
      while(active)
        {
            try
            {
              Event event = listener.take();
              try
              {
                event.execute(this);
              }
              catch(Exception logicException)
              {
                System.out.println(displayName + "fallo al ejecutar un evento: " + logicException.getMessage());
              }
            } 
            catch (InterruptedException e) 
            {
              System.out.println("Tarea interrumpida abruptamente.");
              Thread.currentThread().interrupt();
              break;
            }
        }
    }

}
