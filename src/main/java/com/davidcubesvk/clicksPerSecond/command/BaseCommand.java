package com.davidcubesvk.clicksPerSecond.command;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.command.subcommands.*;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Main plugin command.
 */
public class BaseCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        //Create the CommandUtil
        CommandUtil commandUtil = new CommandUtil(sender, args);

        //No argument specified
        if (args.length == 0) {
            //Invalid format
            commandUtil.invalidFormat();
            return true;
        }

        //Call the sub-command
        switch (args[0].toLowerCase()) {
            case "copy":
                new Copy().onCommand(sender, args, commandUtil);
                return true;
            case "reload":
                new Reload().onCommand(sender, args, commandUtil);
                return true;
            case "scoreboard":
                new Scoreboard().onCommand(sender, args, commandUtil);
                return true;
            case "stats":
                new Stats().onCommand(sender, args, commandUtil);
                return true;
            case "reformat":
                new Reformat().onCommand(sender, args, commandUtil);
                return true;
            case "help":
                new Help().onCommand(sender, args, commandUtil);
                return true;
        }

        //Run the CPS-test command
        if (ClicksPerSecond.getTestCommandExecutor().getCommand().contains("cps") && ClicksPerSecond.getTestCommandExecutor().runCommand(sender, args, true))
            return true;

        //Invalid format
        commandUtil.invalidFormat();
        return true;
    }

}