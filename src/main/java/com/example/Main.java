package com.example;

import com.example.network.*;
import com.example.behaviour.BasicConsole;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main 
{
    public static void main(String[] args) 
    {
        NetworkManager manager = new NetworkManager();
        new Thread(manager).start();

        Map<String, Pc> networkHosts = new HashMap<>();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== SIMULADOR DE RED INICIADO ===");

        while(running) 
        {
            System.out.println("\n--- MODO ADMINISTRADOR (TOPOLOGIA) ---");
            System.out.println("1. Instanciar nueva PC");
            System.out.println("2. Conectar PCs (Crear Link)");
            System.out.println("3. Abrir terminal de una PC");
            System.out.println("4. Apagar simulador");
            System.out.print("Opcion: ");
            
            String option = scanner.nextLine();

            switch (option) 
            {
                case "1":
                    System.out.print("Asigna un nombre a la PC: ");
                    String pcName = scanner.nextLine();
                    
                    if(!networkHosts.containsKey(pcName))
                    {
                        Pc newPc = new Pc(pcName);
                        ((BasicConsole) newPc.console).setup(newPc, manager, networkHosts);
                        networkHosts.put(pcName, newPc);
                        manager.network.addNode(newPc);
                        new Thread(newPc).start();
                        System.out.println("Nodo " + pcName + " creado y encendido.");
                    }
                    else
                    {
                        System.out.println("Error: Ya existe un nodo con ese nombre.");
                    }
                    break;
                    
                case "2":
                    System.out.print("Nodo origen: ");
                    String origin = scanner.nextLine();
                    System.out.print("Nodo destino: ");
                    String dest = scanner.nextLine();
                    
                    Pc pcA = networkHosts.get(origin);
                    Pc pcB = networkHosts.get(dest);
                    
                    if(pcA != null && pcB != null)
                    {
                        manager.network.connectNodes(new Link(pcA, pcB));
                        System.out.println("Enlace de red creado entre " + origin + " y " + dest + ".");
                    }
                    else
                    {
                        System.out.println("Error: Uno o ambos nodos no existen en la red.");
                    }
                    break;
                    
                case "3":
                    System.out.print("Ingresar a la terminal de: ");
                    String targetNode = scanner.nextLine();
                    Pc activePc = networkHosts.get(targetNode);
                    
                    if(activePc != null)
                    {
                        boolean inConsole = true;
                        System.out.println("\n--- CONECTADO A " + targetNode + " ---");
                        System.out.println("Escribe EXIT_CONSOLE para volver al administrador.");
                        
                        while(inConsole)
                        {
                            System.out.print(targetNode + "@root:~$ ");
                            String input = scanner.nextLine();
                            
                            if(input.equalsIgnoreCase("EXIT_CONSOLE"))
                            {
                                inConsole = false;
                                System.out.println("Desconectado de la terminal.");
                            }
                            else
                            {
                                String response = activePc.console.process("User", input);
                                System.out.println(response);
                                
                                try { Thread.sleep(300); } catch (Exception e){} 
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Error: Nodo no encontrado.");
                    }
                    break;
                    
                case "4":
                    running = false;
                    System.out.println("Apagando simulador...");
                    System.exit(0);
                    break;
                    
                default:
                    System.out.println("Opcion no valida.");
            }
        }
        scanner.close();
    }
}