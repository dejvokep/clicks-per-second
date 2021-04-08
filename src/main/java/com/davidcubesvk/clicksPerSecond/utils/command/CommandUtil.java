package com.davidcubesvk.clicksPerSecond.utils.command;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import com.davidcubesvk.clicksPerSecond.utils.replacer.CommandMessageReplacer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * Class containing all basic command functions, like checking for permission, sending a message...
 */
public class CommandUtil {

    //Command sender instance
    private CommandSender sender;
    //Command argument array
    private String[] args;

    /**
     * Initializes this class.
     *
     * @param sender the command sender
     * @param args   the command arguments
     */
    public CommandUtil(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
    }

    /**
     * Sends the command sender a message from the specified path in config.yml.
     *
     * @param path                   the message path
     * @param commandMessageReplacer optional replacers for message
     */
    public void sendMessage(String path, CommandMessageReplacer... commandMessageReplacer) {
        //Do not send if not online
        if ((sender instanceof Player) && !((Player) sender).isOnline())
            return;

        //Loop through all if it is a list
        for (String message : (ClicksPerSecond.getConfiguration().isList(path) ? ClicksPerSecond.getConfiguration().getStringList(path) : Collections.singletonList(ClicksPerSecond.getConfiguration().getString(path)))) {
            //Replace all placeholders
            for (CommandMessageReplacer replacer : commandMessageReplacer)
                message = replacer.replaceInMessage(message);

            //Send the message
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    /**
     * Sends the main command's <code>invalidFormat</code> message.
     */
    public void invalidFormat() {
        //Send the message
        sendMessage("command.main.invalidFormat");
    }

    /**
     * Returns if the command sender is a player.
     * If <code>false</code>, sends the <code>onlyPlayer</code> message.
     *
     * @return if the command sender is a player
     */
    public boolean onlyPlayer() {
        //If player
        boolean player = sender instanceof Player;

        //If not a player
        if (!player)
            //Send the message
            sendMessage("command.onlyPlayer");

        //Return the boolean
        return player;
    }

    /**
     * Returns if the command sender has any of required permissions (full permission - <code>a.b.c</code>, or shorter alternatives - <code>a.b.*</code>...) to run the command.
     * If <code>false</code>, sends the <code>noPermission</code> message.
     *
     * @param permission the full permission
     * @return if the command sender has any of needed permissions
     */
    public boolean hasPermission(String permission) {
        //Boolean to be returned
        boolean hasPermission = false;

        //Check all permissions, also shorter (a.b.c > a.b.* > a.*)
        while (permission.contains(".") && permission.charAt(permission.indexOf('.') + 1) != '*') {
            //Check the permission
            hasPermission = sender.hasPermission(permission);

            //If has permission, do not continue
            if (hasPermission) break;

            //Shorten the permission
            if (permission.endsWith(".*"))
                //Cut the last sub-permission with .* at the end
                permission = permission.substring(permission.substring(0, permission.length() - 2).lastIndexOf('.')) + ".*";
            else
                //Cut the last sub-permission
                permission = permission.substring(0, permission.lastIndexOf('.'));
        }

        //Does not have any of permissions
        if (!hasPermission)
            //Send no permission message
            sendMessage("command.noPermission");

        //Return if has permission
        return hasPermission;
    }

    /**
     * Checks and returns if the length of the command argument array is within the range specified by min and max parameters.
     * If <code>false</code>, sends the main command's <code>invalidFormat</code> message.
     *
     * @param min minimal length of the command argument array
     * @param max maximal length of the command argument array
     * @return if the length of the command argument array is within the min and max range
     */
    public boolean checkArgs(int min, int max) {
        //Check if the length of the argument array is within the range
        boolean inRange = args.length <= max && args.length >= min;

        //If not within the range
        if (!inRange)
            //Send invalid format message
            invalidFormat();

        return inRange;
    }

    /**
     * Returns <code>true</code> if:
     * <ul>
     * <li>the data format in currently used data storage is outdated</li>
     * <li>the reformatting process is active</li>
     * <li>the format version is being got, e.g. if {@link Reformatter#isLoaded()} returns <code>false</code></li>
     * </ul>
     * If any of these statements are true, sends a message representing the current status to the command sender.
     *
     * @return if the data format is outdated depending on the statements listed above
     */
    public boolean isFormatOutdated() {
        //If the format version is being obtained
        if (!Reformatter.getInstance().isLoaded()) {
            sendMessage("command.format.gettingVersion");
            return true;
        }

        //If the format is outdated
        if (Reformatter.getInstance().isReformatNeeded()) {
            sendMessage("command.format.outdated");
            return true;
        }

        //If reformatting
        if (ClicksPerSecond.getStorageOperatorByType(ClicksPerSecond.getStorageType()).isReformatActive()) {
            sendMessage("command.format.reformatting");
            return true;
        }

        return false;
    }
}
