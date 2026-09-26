package com.example.network;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import com.example.behaviour.Console;

public class Pc 
{
    /*
        ¿Que se supone que debe llevar una PC(nodo) en este caso?

        IP:
        - Esto hace que se reconozca con otros nodos en su misma red
        - Pensaba simular una sola Red dentro del simulador pero tal vez permita hacer distintas redes dentro de la propia simulacion
          sin entrarnos con servidores, el servidor quedaria embebido y seria seleccionable para cada PC mediante un menu desplegable
        - debe ser valida
        - Tipo de dato: Por asignar (SubnetUtils?)

        MAC:
        - No se realmente si sea necesaria para el simulador tan limitado que estamos diseñando, pero sirve como identificador de la PC
          ya que tiene que ser unico asi que lo podemos usar como un ID
        - Tipo de dato: Por asignar

        Display_Name:
        - Es normal poder cambiarle el nombre a un equipo en el simulador y no amplia el tiempo de desarrollo
        - String

        Connections:
        - Aunque parece logico implementar las conexiones en esta parte en realidad estas deben ser propias del grafo
          pues si las metemos en la PC el grafo de JGraphT no funcionaria y romperiamos un poco la magia del polimorfismo

        Consola:
        - Cada PC debe guardar su registro de consola 
        - Tipo de dato: Class Console

        Conmutacion Por Circuitos:
        - Necesitamos guardar la disponibilidad de la PC, al hacer circuitos las rutas se reservan, por lo que el grafo debe saber si un nodo esta disponible o no
        - Usaremos un ENUM sencillo {DISPONIBLE, OCUPADO, APAGADO}

        Conmutación Por Paquetes:
        - Es obligatorio incluir una cola para el orden de los paquetes en este metodo
        - Usaremos un Queue
    */

    String IP, MAC, displayName;
    Console console;
    enum Status{AVAILABLE,BUSY,OFF};
    Status status;
    Queue<Packt> packages = new LinkedList<>();

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

    public void setOff() 
    {
        this.status = Status.OFF;
    }

    public void setAvailable()
    {
        this.status = Status.AVAILABLE;
    }
}
