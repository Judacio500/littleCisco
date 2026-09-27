package com.example;
import com.example.network.JGraphTNetwork;
import com.example.network.Pc;
import com.example.network.Link;

/*
    CONMUTACION POR CIRCUITOS

    Es necesario desarrollar un sistema que haga lo siguiente:

    1.-Seleccionar una PC
    2.-Abrir su terminal
    3.-Escribir comandos (requests) a otros nodos en la red
    4.-trazar una ruta hacia la PC a la que se le hizo la solicitud
    5.-hacer llegar el paquete
    6.-recibir una respuesta por la misma ruta
    7.-soltar el circuito
*/

public class Main {
    public static void main(String[] args) 
    {
        JGraphTNetwork pcNetwork = new JGraphTNetwork();

        Pc p1 = new Pc("prueba_1");
        Pc p2 = new Pc("prueba_2");
        Pc p3 = new Pc("prueba_3");

        Link p1p2 = new Link(p1,p2);
        Link p1p3 = new Link(p1,p3);

        pcNetwork.addNode(p1);
        pcNetwork.addNode(p2);
        pcNetwork.addNode(p3);

        pcNetwork.connectNodes(p1p2);
        pcNetwork.connectNodes(p1p3);

        System.out.println(pcNetwork.toString());
    }
}