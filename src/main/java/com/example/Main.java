package com.example;

import java.util.Scanner;
import com.example.network.Link;
import com.example.network.TopologySandbox;
import com.example.network.devices.NIC;
import com.example.network.devices.NetworkNode;
import com.example.network.devices.NodeFactory;
import com.example.network.devices.Pc;
import com.example.network.devices.Router;
import com.example.network.devices.SubnetUtils;

public class Main
{
    public static void main(String[] args)
    {
        TopologySandbox sandbox = new TopologySandbox();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while(running)
        {
            System.out.println("\n--- MODO ADMINISTRADOR (TOPOLOGIA) ---");
            System.out.println("1. Correr suite completa (Topologia inicial + OSPF 1.jg con 5 Routers -> LOG en PC_A)");
            System.out.println("2. Correr solo topologia OSPF 1.jg (5 Routers Multi-Area + ABR + ASBR -> LOG en PC_A)");
            System.out.println("3. Instanciar nueva PC o Router");
            System.out.println("4. Agregar interfaz (NIC) a un nodo");
            System.out.println("5. Conectar interfaces (Crear Link)");
            System.out.println("6. Abrir terminal de un nodo (PC o Router)");
            System.out.println("7. Ver estado de topologia y enlaces");
            System.out.println("8. Apagar simulador");
            System.out.print("Opcion: ");

            String option = scanner.nextLine().trim();

            switch(option)
            {
                case "1":
                    System.out.println("Ejecutando suite completa (Estatica + OSPF 5 Routers)...");
                    sandbox.runFullVerificationSuite();
                    System.out.println("Listo. Abre la terminal de PC_A (Opcion 6 -> PC_A) y ejecuta LOG.");
                    break;

                case "2":
                    System.out.println("Ejecutando topologia OSPF 1.jg de 5 Routers...");
                    sandbox.runOspfFiveRouterSuite(null);
                    System.out.println("Listo. Abre la terminal de PC_A (Opcion 6 -> PC_A) y ejecuta LOG.");
                    break;

                case "3":
                    System.out.print("Tipo (PC / ROUTER): ");
                    String type = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Nombre del nodo: ");
                    String name = scanner.nextLine().trim();
                    if(name.isEmpty())
                    {
                        System.out.println("Error: Nombre vacio.");
                        break;
                    }
                    if("PC".equals(type))
                    {
                        sandbox.registerNode(NodeFactory.createPc(name));
                        System.out.println("PC '" + name + "' instanciada.");
                    }
                    else if("ROUTER".equals(type))
                    {
                        sandbox.registerNode(NodeFactory.createRouter(name));
                        System.out.println("Router '" + name + "' instanciado.");
                    }
                    else
                    {
                        System.out.println("Error: Tipo no valido.");
                    }
                    break;

                case "4":
                    System.out.print("Nombre del nodo: ");
                    NetworkNode targetNode = sandbox.getNode(scanner.nextLine());
                    if(targetNode == null)
                    {
                        System.out.println("Error: Nodo no encontrado.");
                        break;
                    }
                    System.out.print("Nombre de interfaz (ej. eth0): ");
                    String nicName = scanner.nextLine().trim();
                    System.out.print("Direccion IP (Enter para dejar sin IP): ");
                    String ip = scanner.nextLine().trim();
                    if(ip.isEmpty())
                    {
                        targetNode.addInterface(new NIC(nicName, null, null));
                        System.out.println("Interfaz " + nicName + " agregada sin IP.");
                    }
                    else
                    {
                        System.out.print("Mascara de subred (ej. 255.255.255.0): ");
                        String mask = scanner.nextLine().trim();
                        if(!SubnetUtils.isValidIp(ip) || !SubnetUtils.isValidIp(mask))
                        {
                            System.out.println("Error: IP o mascara invalida.");
                            break;
                        }
                        targetNode.addInterface(new NIC(nicName, ip, mask));
                        System.out.println("Interfaz " + nicName + " (" + ip + "/" + SubnetUtils.maskToPrefixLength(mask) + ") agregada.");
                    }
                    break;

                case "5":
                    System.out.print("Nodo A: ");
                    NetworkNode nodeA = sandbox.getNode(scanner.nextLine());
                    System.out.print("Interfaz de Nodo A: ");
                    String nicAName = scanner.nextLine().trim();

                    System.out.print("Nodo B: ");
                    NetworkNode nodeB = sandbox.getNode(scanner.nextLine());
                    System.out.print("Interfaz de Nodo B: ");
                    String nicBName = scanner.nextLine().trim();

                    if(nodeA == null || nodeB == null)
                    {
                        System.out.println("Error: Uno o ambos nodos no existen.");
                        break;
                    }
                    NIC portA = nodeA.getInterfaceByName(nicAName);
                    NIC portB = nodeB.getInterfaceByName(nicBName);
                    if(portA == null || portB == null)
                    {
                        System.out.println("Error: Interfaz no encontrada.");
                        break;
                    }
                    System.out.print("Costo/Peso del enlace (ej. 10): ");
                    double weight = 10.0;
                    try { weight = Double.parseDouble(scanner.nextLine().trim()); }
                    catch(NumberFormatException ignored) {}

                    String linkId = "LNK-" + nodeA.getDisplayName() + "-" + nodeB.getDisplayName();
                    sandbox.getNetwork().addLink(portA, portB, new Link(linkId), weight);
                    System.out.println("Enlace " + linkId + " (costo " + (int) weight + ") conectado.");
                    break;

                case "6":
                    System.out.print("Ingresar a la terminal de: ");
                    NetworkNode consoleNode = sandbox.getNode(scanner.nextLine());
                    if(consoleNode == null)
                    {
                        System.out.println("Error: El nodo no existe.");
                        break;
                    }
                    openNodeTerminal(scanner, consoleNode);
                    break;

                case "7":
                    printTopologyStatus(sandbox);
                    break;

                case "8":
                    running = false;
                    sandbox.shutdown();
                    System.out.println("Simulador apagado.");
                    break;

                default:
                    System.out.println("Opcion no valida.");
            }
        }
        scanner.close();
    }

    private static void openNodeTerminal(Scanner scanner, NetworkNode node)
    {
        String promptSuffix = (node instanceof Router) ? "#" : "@root:~$";
        System.out.println("\n--- CONECTADO A " + node.getDisplayName() + " ---");
        System.out.println("Escribe EXIT_CONSOLE para volver al administrador (HELP para comandos).");

        while(true)
        {
            System.out.print(node.getDisplayName() + promptSuffix + " ");
            String cmd = scanner.nextLine().trim();
            if(cmd.equalsIgnoreCase("EXIT_CONSOLE"))
            {
                System.out.println("Desconectado de la terminal.");
                break;
            }
            if(cmd.isEmpty()) continue;

            String output = "";
            if(node instanceof Pc) output = ((Pc) node).console.process("User", cmd);
            else if(node instanceof Router) output = ((Router) node).console.process("Admin", cmd);

            if(output != null && !output.isEmpty()) System.out.println(output);
        }
    }

    private static void printTopologyStatus(TopologySandbox sandbox)
    {
        System.out.println("\n--- ESTADO DE TOPOLOGIA ---");
        if(sandbox.getNodes().isEmpty())
        {
            System.out.println("(Sin nodos instanciados)");
            return;
        }
        for(NetworkNode node : sandbox.getNodes().values())
        {
            String type = (node instanceof Router) ? "ROUTER" : "PC";
            System.out.print("[" + type + "] " + node.getDisplayName() + " -> ");
            for(NIC nic : node.getInterfaces())
            {
                System.out.print(nic.getName() + "(" + (nic.getIpAddress() != null ? nic.getIpAddress() : "sin IP") + ") ");
            }
            System.out.println();
        }
        System.out.println("Enlaces: " + sandbox.getNetwork().getGraph().edgeSet().size());
    }
}