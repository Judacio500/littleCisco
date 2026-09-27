package com.example.network;

import java.util.List;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import com.example.interfaces.Network;
import java.util.Map;
import java.util.HashMap;

public class JGraphTNetwork implements Network
{
    SimpleWeightedGraph<Pc, DefaultWeightedEdge> network = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    Map<DefaultWeightedEdge, Link> links = new HashMap<>();

    public JGraphTNetwork()
    {
        
    }

    public void addNode(Pc pc) 
    {
        network.addVertex(pc);
    }

    public void connectNodes(Link connection) 
    {
        DefaultWeightedEdge jgraphtEdge = network.addEdge(connection.endPointA, connection.endPointB);
        links.put(jgraphtEdge,connection);
        network.setEdgeWeight(jgraphtEdge, connection.getWeight());
    }
    
    public List<Link> getRouting(Pc origin, Pc destination) 
    {
        return null;
    }

    @Override 
    public String toString()
    {
        return network.toString();
    }
       
}
