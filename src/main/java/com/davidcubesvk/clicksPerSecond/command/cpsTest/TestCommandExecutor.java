package com.davidcubesvk.clicksPerSecond.command.cpsTest;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.test.Test;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test commands's logic executor.
 */
public class TestCommandExecutor {

    //Main command, start and end sub-commands
    private Set<String> command = new HashSet<>(), startSub = new HashSet<>(), endSub = new HashSet<>();
    //Maximum argument length
    private int maxArgs = 0;
    //Allowed worlds
    private List<String> allowedWorlds = new ArrayList<>();

    /**
     * Calls {@link #reload()} to load all internal variables.
     */
    public TestCommandExecutor() {
        reload();
    }

    /**
     * Returns if the command was run (if the used syntax matches this command).
     *
     * @param sender              the sender
     * @param args                the command arguments
     * @param usedByPluginCommand if called by /cps command executor
     * @return if the command was run
     */
    public boolean runCommand(CommandSender sender, String[] args, boolean usedByPluginCommand) {
        //Create command util instance
        CommandUtil commandUtil = new CommandUtil(sender, args);

        //If there's any match of the used command
        boolean match = matchCommand(args);
        //If not and this is executed by /cps command executor, return no match
        if (!match && usedByPluginCommand)
            return false;

        //If sender is in allowed world
        if (!checkWorld(sender, commandUtil)) {
            commandUtil.sendMessage("command.test.world.notAllowedWorld");
            return true;
        }

        //If the sender does not have permission, is not a player, or the format is outdated
        if (!commandUtil.hasPermission("cps.testCommand") || !commandUtil.onlyPlayer() || commandUtil.isFormatOutdated())
            return true;

        //If none of the sub-commands match
        if (!match) {
            //Send message and return
            commandUtil.sendMessage("command.test.invalidFormat");
            return false;
        }

        //Cast to player
        Player player = (Player) sender;
        //Get manager instance
        TestManager manager = TestManager.getInstance();
        //If the player is in a test
        boolean isInTest = manager.isInTest(player.getUniqueId());

        //If no sub-commands are configured
        if (maxArgs == 0) {
            //If in the test, end, otherwise start a new test
            if (isInTest)
                manager.endTest(player, Test.EndCause.CANCEL);
            else
                manager.startTest(player);

            return true;
        }

        //If using the start command syntax
        if ((args.length == 0 && startSub.size() == 0) || (args.length > 0 && startSub.contains(args[0]))) {
            //If in a test
            if (isInTest) {
                //Send message and return
                commandUtil.sendMessage("command.test.running");
                return true;
            }

            //Start a new test
            manager.startTest(player);
            return true;
        }

        //If using the end command syntax
        if ((args.length == 0 && endSub.size() == 0) || (args.length > 0 && endSub.contains(args[0]))) {
            //If not in any test
            if (!isInTest) {
                //Send message and return
                commandUtil.sendMessage("command.test.noRunning");
                return true;
            }

            //End the test
            manager.endTest(player, Test.EndCause.CANCEL);
            return true;
        }

        return true;
    }

    /**
     * Returns if there's any match in command syntax to CPS-test command's syntax.
     *
     * @param args the command arguments
     * @return if there's any match in command syntax to CPS-test command's syntax
     */
    private boolean matchCommand(String[] args) {
        //If invalid argument length is used
        if (maxArgs < args.length)
            return false;

        //If no sub-commands are configured and no sub-command is used (checked in the first if statement), return true
        if (maxArgs == 0)
            return true;

        //If an action is bound only to the main command and no sub-command is used
        if ((startSub.size() == 0 || endSub.size() == 0) && args.length == 0)
            return true;

        //True if a sub-command matches the used one, otherwise false
        return startSub.contains(args[0]) || endSub.contains(args[0]);
    }

    /**
     * Reloads CPS test command's internal variables.
     */
    public void reload() {
        //Get config
        FileConfiguration config = ClicksPerSecond.getConfiguration();

        //Main command
        command = new HashSet<>(config.getStringList("test.command.main"));
        //Start and end sub-commands
        startSub = new HashSet<>(config.getStringList("test.command.sub.start"));
        endSub = new HashSet<>(config.getStringList("test.command.sub.cancel"));
        //Allowed worlds
        allowedWorlds = config.getStringList("command.test.world.worlds");

        //Initialize maximum argument array length
        maxArgs = Math.max(startSub.size(), endSub.size()) > 0 ? 1 : 0;
    }

    /**
     * Returns if the sender can run the CPS-test command.
     * More formally, checks if the sender is in any of allowed worlds, or if has the bypass permission.
     *
     * @param sender      the sender
     * @param commandUtil a command utility instance used to check permission and it's parent versions
     * @return if the sender is in any of allowed worlds (or if has the bypass permission)
     */
    private boolean checkWorld(CommandSender sender, CommandUtil commandUtil) {
        //Check the bypass permission and if the sender is console
        if (commandUtil.hasPermission("cps.bypass.world") || sender instanceof ConsoleCommandSender)
            return true;

        //If all worlds are allowed
        if (allowedWorlds.size() == 1)
            if (allowedWorlds.get(0).equals("*"))
                return true;

        //Return true if player's world is allowed world
        return allowedWorlds.contains(((Player) sender).getLocation().getWorld().getName());
    }

    /**
     * Returns the command names configured in config.yml as CPS-test commands.
     *
     * @return the command names configured in config.yml as CPS-test commands
     */
    public Set<String> getCommand() {
        return command;
    }
}
