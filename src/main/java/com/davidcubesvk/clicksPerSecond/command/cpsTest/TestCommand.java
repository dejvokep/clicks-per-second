package com.davidcubesvk.clicksPerSecond.command.cpsTest;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Test commands's server executor.
 */
public class TestCommand extends Command {

    /**
     * Initializes the command with the given name.
     *
     * @param name the command name
     */
    public TestCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        //Run command
        ClicksPerSecond.getTestCommandExecutor().runCommand(sender, args, false);
        return true;
    }
}
