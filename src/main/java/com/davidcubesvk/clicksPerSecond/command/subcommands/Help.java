package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.command.CommandSender;

/**
 * Handler for help sub-command.
 */
public class Help implements CommandProcessor {

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        //Check the permission and args
        if (!commandUtil.hasPermission("cps.command.help") || !commandUtil.checkArgs(1, 1))
            return;

        //Send the help page
        commandUtil.sendMessage("command.main.help");
    }

}
