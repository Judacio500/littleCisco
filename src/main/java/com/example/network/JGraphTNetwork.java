package com.example.network;

import java.util.List;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import com.example.interfaces.Network;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import java.util.Map;
import java.util.ArrayList;
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
        KShortestPathAlgorithm<Pc, DefaultWeightedEdge> djk = new YenKShortestPath<>(network);
        List<GraphPath<Pc,DefaultWeightedEdge>> posibleConnections = djk.getPaths(origin, destination,5);

        List<Link> connectionBuffer = new ArrayList<>();

        for(GraphPath<Pc,DefaultWeightedEdge> infoRoute : posibleConnections)
        {
            List<DefaultWeightedEdge> connectionList = infoRoute.getEdgeList();

            for(DefaultWeightedEdge edge : connectionList)
            {
                Link currentLink = links.getOrDefault(edge, null);
                if(currentLink.isAvailable()) // Si todos los cables estan disponibles el for terminara normalmente
                    connectionBuffer.add(currentLink);
                else
                {
                    connectionBuffer.clear();
                    break;  // En cambio si uno de los cables no esta disponible el ciclo
                              // se detiene aqui y pasa a inspeccionar la siguiente ruta
                }
            }
            if(!connectionBuffer.isEmpty()) // Si el ciclo termino, antes de pasar a la siguiente ruta
                break;                      // preguntamos si la lista tiene elementos ya que el unico caso donde no los tiene es cuando un cable no estaba disponible
                                            // lo que significa que si tiene elementos tiene una ruta disponible y no tiene que checar las otras
        }   
        return connectionBuffer;
    }

    @Override 
    public String toString()
    {
        return network.toString();
    }
       
}
