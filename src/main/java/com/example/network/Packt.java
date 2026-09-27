package com.example.network;

import java.util.List;
import com.example.network.Link;

public class Packt 
{
    int id;
    int order;
    String Message;
    List<Link> route; 

    public Packt(String Message, int id, List<Link> route)
    {
        this.Message = Message;
        this.id = id;
        this.route = route;
        this.order = -1;
    }

    public Packt(String Message, int id, int order, List<Link> route)
    {
        this.Message = Message;
        this.id = id;
        this.route = route;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    

}
