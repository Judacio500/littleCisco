package com.example.interfaces;
import java.util.List;

import com.example.network.Link;
import com.example.network.Pc;

public interface Network 
{
    void addNode(Pc pc);
    void connectNodes(Link connection);
    List<Link> getRouting(Pc origin, Pc destination);
}
