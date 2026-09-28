package com.example.network;
import java.util.concurrent.ThreadLocalRandom;


public class Link
{
    /*
        ¿Que necesita un Cable(arista)?

        Los cables son el analogo de las aristas del grafo, representan la conexion entre 2 Pc

        Extremos:
        A - B
        - Debemos poder conectar 2 PCs mediante un cable

        Estado del cable:
        - Esta siendo utilizado para transmitir datos actualmente?
        - Esta libre para transmision?
        - Se rompio el cable?

        Peso:
        - Los algoritmos de grafos utilizan peso para poder tomar decisiones
        - Ademas nos permitira añadir latencia si el simulador crece
    */

    Pc endPointA, endPointB;
    enum Status{AVAILABLE, BUSY, BROKEN};
    Status status;
    double weight;


    public Link(Pc A, Pc B)
    {
        this.endPointA = A;
        this.endPointB = B;
        this.status = Status.AVAILABLE;
        this.weight = ThreadLocalRandom.current().nextDouble(10, 250.0); // Random delay between 10 and 250 ms
    }

    public Link(Pc A, Pc B, double weight)
    {
        this.endPointA = A;
        this.endPointB = B;
        this.weight = weight;
        this.status = Status.AVAILABLE;
    }

    public double getWeight() 
    {
        return weight;
    }

    public void setEndPointA(Pc endPointA) 
    {
        this.endPointA = endPointA;
    }

    public void setEndPointB(Pc endPointB) 
    {
        this.endPointB = endPointB;
    }

    public void setBroken() 
    {
        this.status = Status.BROKEN;
    }

    public void setAvailable()
    {
        this.status = Status.AVAILABLE;
    }

    public void setBusy()
    {
        this.status = Status.BUSY;
    }

    public void setWeight(double weight) 
    {
        if(weight >= 2000) // Ponemos como valor maximo 2000ms
        {
            this.weight = 2000;
        }
        else
        {
            this.weight = weight;
        }
        return;
    }

    public Pc getEndPointA() {
        return endPointA;
    }

    public Pc getEndPointB() {
        return endPointB;
    }  

    public Boolean isAvailable()
    {
        if(this.status == Status.AVAILABLE)
            return true;
        return false;
    }
}
