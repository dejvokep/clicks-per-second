package com.davidcubesvk.clicksPerSecond.command;

import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.command.CommandSender;

/**
 * Interface used to execute a sub-command.
 */
public interface CommandProcessor {

    /**
     * Runs the sub-command.
     *
     * @param sender      sender of this command
     * @param args        command arguments in an array
     * @param commandUtil a command utility created in the main command executor
     */
    void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil);

}
