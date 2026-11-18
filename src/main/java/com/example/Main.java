package com.example;

import java.util.Scanner;
import com.example.network.Link;
import com.example.network.TopologySandbox;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.NodeFactory;
import com.example.network.devices.Pc;
import com.example.network.devices.RouteEntry;
import com.example.network.devices.Router;

public class Main
{
    public static void main(String[] args)
    {
        TopologySandbox sandbox = new TopologySandbox();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== SIMULADOR DE RED MODULAR INICIADO ===");

        while(running)
        {
            System.out.println("\n--- MODO ADMINISTRADOR (TOPOLOGIA) ---");
            System.out.println("1. Cargar topologia multi-salto (PC_A, PC_C <-> Router_1 <-> PC_B) y correr pruebas");
            System.out.println("2. Instanciar nueva PC o Router");
            System.out.println("3. Agregar interfaz (NIC) a un nodo");
            System.out.println("4. Conectar interfaces (Crear Link)");
            System.out.println("5. Agregar ruta estatica a un nodo");
            System.out.println("6. Abrir terminal de una PC");
            System.out.println("7. Ver estado de topologia y enlaces");
            System.out.println("8. Apagar simulador");
            System.out.print("Opcion: ");

            String option = scanner.nextLine().trim();

            switch(option)
            {
                case "1":
                    if(sandbox.getAllNodes().isEmpty())
                    {
                        sandbox.buildDefaultMultiHopTopology();
                        System.out.println("Topologia multi-salto desplegada y encendida.");
                    }
                    sandbox.runVerificationSuite();
                    break;

                case "2":
                    System.out.print("Tipo de dispositivo (PC / ROUTER): ");
                    String type = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Nombre del nodo: ");
                    String nodeName = scanner.nextLine().trim();

                    if(sandbox.getNode(nodeName) != null)
                    {
                        System.out.println("Error: Ya existe un nodo con ese nombre.");
                        break;
                    }

                    System.out.print("IP para interfaz inicial eth0: ");
                    String initialIp = scanner.nextLine().trim();

                    NetworkNode newNode = "ROUTER".equals(type)
                        ? NodeFactory.createRouter(nodeName)
                        : NodeFactory.createPc(nodeName);

                    newNode.addInterface(new NIC("eth0", initialIp));
                    sandbox.registerNode(newNode);
                    System.out.println(type + " " + nodeName + " creado en " + initialIp + " y encendido.");
                    break;

                case "3":
                    System.out.print("Nombre del nodo: ");
                    NetworkNode targetForNic = sandbox.getNode(scanner.nextLine().trim());
                    if(targetForNic == null)
                    {
                        System.out.println("Error: Nodo no encontrado.");
                        break;
                    }
                    System.out.print("Nombre de interfaz (ej. eth1): ");
                    String nicName = scanner.nextLine().trim();
                    System.out.print("Direccion IP: ");
                    String nicIp = scanner.nextLine().trim();

                    targetForNic.addInterface(new NIC(nicName, nicIp));
                    sandbox.refreshAllConsoles();
                    System.out.println("Interfaz " + nicName + " (" + nicIp + ") agregada a " + targetForNic.getDisplayName() + ".");
                    break;

                case "4":
                    System.out.print("Nodo A: ");
                    NetworkNode nodeA = sandbox.getNode(scanner.nextLine().trim());
                    System.out.print("Interfaz A (ej. eth0): ");
                    String portAName = scanner.nextLine().trim();

                    System.out.print("Nodo B: ");
                    NetworkNode nodeB = sandbox.getNode(scanner.nextLine().trim());
                    System.out.print("Interfaz B (ej. eth0): ");
                    String portBName = scanner.nextLine().trim();

                    if(nodeA == null || nodeB == null)
                    {
                        System.out.println("Error: Uno o ambos nodos no existen.");
                        break;
                    }

                    NIC portA = nodeA.getInterfaceByName(portAName);
                    NIC portB = nodeB.getInterfaceByName(portBName);
                    if(portA == null || portB == null)
                    {
                        System.out.println("Error: Interfaz no encontrada en alguno de los nodos.");
                        break;
                    }

                    System.out.print("Peso del enlace (ej. 10): ");
                    double weight = Double.parseDouble(scanner.nextLine().trim());
                    String linkId = "LNK-" + nodeA.getDisplayName() + "-" + nodeB.getDisplayName();

                    sandbox.getNetwork().addLink(portA, portB, new Link(linkId), weight);
                    System.out.println("Enlace " + linkId + " conectado.");
                    break;

                case "5":
                    System.out.print("Nodo a configurar: ");
                    NetworkNode routeNode = sandbox.getNode(scanner.nextLine().trim());
                    if(routeNode == null)
                    {
                        System.out.println("Error: Nodo no encontrado.");
                        break;
                    }
                    System.out.print("IP Destino (o 0.0.0.0 para ruta por defecto): ");
                    String destIp = scanner.nextLine().trim();
                    System.out.print("IP Siguiente Salto (dejar vacio si es ruta directa): ");
                    String nextHop = scanner.nextLine().trim();
                    System.out.print("Interfaz de salida (ej. eth0): ");
                    NIC outNic = routeNode.getInterfaceByName(scanner.nextLine().trim());

                    if(outNic == null)
                    {
                        System.out.println("Error: Interfaz invalida.");
                        break;
                    }

                    RouteEntry entry = nextHop.isEmpty()
                        ? RouteEntry.createDirectRoute(destIp, outNic)
                        : new RouteEntry(destIp, nextHop, outNic, 1);

                    routeNode.addRoute(entry);
                    System.out.println("Ruta agregada en " + routeNode.getDisplayName() + ".");
                    break;

                case "6":
                    System.out.print("Ingresar a la terminal de: ");
                    NetworkNode selected = sandbox.getNode(scanner.nextLine().trim());
                    if(!(selected instanceof Pc))
                    {
                        System.out.println("Error: El nodo no existe o no es una PC.");
                        break;
                    }

                    Pc activePc = (Pc) selected;
                    boolean inConsole = true;
                    System.out.println("\n--- CONECTADO A " + activePc.getDisplayName() + " ---");
                    System.out.println("Escribe EXIT_CONSOLE para volver al administrador.");

                    while(inConsole)
                    {
                        System.out.print(activePc.getDisplayName() + "@root:~$ ");
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
                            try
                            {
                                Thread.sleep(250);
                            }
                            catch(InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    break;

                case "7":
                    System.out.println("\n--- NODOS ACTIVOS ---");
                    for(NetworkNode n : sandbox.getAllNodes())
                    {
                        String role = (n instanceof Router) ? "ROUTER" : "PC";
                        System.out.print("[" + role + "] " + n.getDisplayName() + " -> ");
                        for(NIC nic : n.getInterfaces())
                        {
                            System.out.print(nic.getName() + ":" + nic.getIpAddress() + " (" + nic.getMacAddress() + ") ");
                        }
                        System.out.println();
                    }
                    System.out.println("\n--- ENLACES ---");
                    for(Link link : sandbox.getNetwork().getGraph().edgeSet())
                    {
                        String state = link.isAvailable() ? "LIBRE" : "RESERVADO (" + link.getCircuitId() + ")";
                        System.out.println(link.getId() + " | Peso: " + link.getLinkWeight() + " | Estado: " + state);
                    }
                    break;

                case "8":
                    running = false;
                    System.out.println("Apagando nodos y liberando hilos...");
                    sandbox.shutdownAll();
                    break;

                default:
                    System.out.println("Opcion no valida.");
            }
        }

        scanner.close();
    }
}