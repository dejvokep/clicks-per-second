package com.davidcubesvk.clicksPerSecond.utils.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;

/**
 * Class used to register commands dynamically (with the server turned on) without them being specified in plugin.yml.
 */
public class DynamicCommandRegisterer {

    //CommandMap instance
    private CommandMap commandMap;

    /**
     * Initializes the command map instance.
     */
    public DynamicCommandRegisterer() {
        //Get command map
        try {
            //Get field
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            //Set accessible
            field.setAccessible(true);
            //Set value
            commandMap = (CommandMap) field.get(Bukkit.getServer().getPluginManager());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Registers the specified command.
     *
     * @param command the command to register
     */
    public void register(Command command) {
        //Register command
        commandMap.register("_", command);
    }

}
