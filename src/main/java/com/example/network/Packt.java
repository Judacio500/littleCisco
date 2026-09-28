package com.example.network;

import java.util.UUID;
import java.util.List;
import com.example.network.Link;

public class Packt 
{
    UUID id;
    int order;
    int nParts;
    String Message;
    public List<Link> route; 

    public Packt(String Message, List<Link> route)
    {
        this.Message = Message;
        this.id = UUID.randomUUID();
        this.route = route;
        this.nParts = 1;
        this.order = 0;
    }

    public Packt(String Message, UUID id, int parts, int order, List<Link> route)
    {
        this.Message = Message;
        this.id = id;
        this.route = route;
        this.order = order;
        this.nParts = parts;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public List<Link> getRoute() {
        return route;
    }

    public void setRoute(List<Link> route) {
        this.route = route;
    }

    public int getnParts() {
        return nParts;
    }

    public void setnParts(int nParts) {
        this.nParts = nParts;
    }

}
