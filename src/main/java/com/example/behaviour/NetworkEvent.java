package com.example.behaviour;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.interfaces.Event;
import com.example.network.Pc;
import com.example.network.Packt;
import com.example.network.Link;

public class NetworkEvent implements Event
{
    Packt data;

    public void execute(Pc context)
    {
        if(data.route.isEmpty())
        {
            // Si la lista esta vacia entonces esta es la PC final del trayecto y tiene la obligacion de resolver el paquete
            solvePacket(context);
            return;
        }
        // Si no esta vacia es porque el paquete aun se puede pasar a una PC más
        passPacket(context);
    }    

    // Limpio y lindo, ordena n cantidad de paquetes donde n = {1,4 bytes} (4 bytes es el tamaño del integer que usamos para el atributo)
    // Como el numero de paquetes puede ser 1 el codigo lo trata igual que todo otro paquete
    // solo que el mensaje final se ensambla con una sola pieza, y con esto matamos 2 pajaros de 1 tiro
    // Paquetes y Circuitos reconstruidos con el mismo algoritmo 
    public void solvePacket(Pc context)
    {
        List<Packt> packtList = context.packtBuffer.computeIfAbsent(data.getId(), k -> new ArrayList<>());
    
        packtList.add(data);

        if(packtList.size() == data.getnParts())
        {
            packtList.sort(Comparator.comparingInt(Packt::getOrder));
            StringBuilder message = new StringBuilder();
            for(Packt p : packtList)
            {
                message.append(p.getMessage());
            }
            // Escribe el mensaje reconstruido en la consola de esta PC 
            context.console.log("System", message.toString());
        }
    }

    public void passPacket(Pc context)
    {
        Link current = data.route.remove(0); 
        
        Pc endPointA = current.getEndPointA();
        Pc endPointB = current.getEndPointB();
        
        Pc nextHop = context.equals(endPointA) ? endPointB : endPointA;
        
        // 4. Inyectamos el evento en el buzón del vecino
        nextHop.queueInstruction(this);
    }
}
