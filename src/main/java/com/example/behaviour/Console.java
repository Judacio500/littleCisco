package com.example.behaviour;

import java.util.ArrayList;
import java.util.List;

public abstract class Console
{
    protected List<String> consoleLog = new ArrayList<>();

    public String process(String user, String command)
    {
        // Simplemente retorna el mensaje en mayusculas
        this.log(user,command);
        String response = command.toUpperCase();
        this.log("System",response);
        return response;
    }
    
    public void log(String entity, String command)
    {
        String newEntry = entity + ": " + command;
        consoleLog.add(newEntry);
        return;
    }
}
