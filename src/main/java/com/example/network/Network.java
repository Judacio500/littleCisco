package com.example.network;
import java.util.List;

public interface Network 
{
    void addNode(Pc pc);
    void connectNodes(Link connection);
    List<Link> getRouting(Pc origin, Pc destination);
}
