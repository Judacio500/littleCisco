package com.example;

public class Cable
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
    double weight;
}
