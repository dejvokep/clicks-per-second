package com.davidcubesvk.clicksPerSecond.utils.action;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.utils.replacer.PlayerStringReplacer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Class covering all action types and their execution.
 */
public class Action {

    /**
     * Enum representing all action types.
     * - CONSOLE_COMMAND: runs a command as console
     * - COMMAND: runs a command as player
     * - MESSAGE: sends a message to the player
     * - PARTICLE: spawns a particle effect in a world which can be seen only by the player
     * - PARTICLE_PUBLIC: spawns a particle effect in a world which can be seen by everyone
     * - SOUND: plays a sound in a world which can be heard only by the player
     * - SOUND_PUBLIC: plays a sound in a world which can be heard by everyone
     * - TITLE: sends a title and sub-title to the player
     * - ACTIONBAR: sends an actionbar message to the player
     * - CHAT: chats as the player
     * - TELEPORT: teleports the player to a location
     * - EXP_LEVEL: sets the player's experience to some level
     * - WRONG_ACTION_SPECIFICATION: represents an action that was specified wrongly
     */
    public enum Type {
        CONSOLE_COMMAND, COMMAND, MESSAGE,
        PARTICLE, PARTICLE_PUBLIC, SOUND, SOUND_PUBLIC, TITLE,
        ACTIONBAR, CHAT, TELEPORT,
        EXP_LEVEL, WRONG_ACTION_SPECIFICATION
    }

    //Split string
    private static final String SPLIT_STRING = "///";

    //Actions to be ran
    private Collection<String> actions;

    /**
     * Initializes this class from the specified List containing action strings.
     *
     * @param actions the action strings
     */
    public Action(List<String> actions) {
        //Set actions variable
        this.actions = actions;
    }

    /**
     * Runs actions for the specified player.
     *
     * @param player          the receiver
     * @param stringReplacers optional replacers for action strings
     */
    public void run(Player player, PlayerStringReplacer... stringReplacers) {
        //Do not run if offline
        if (!player.isOnline())
            return;

        //Action data for error reporting
        ActionData actionData = new ActionData(Type.WRONG_ACTION_SPECIFICATION, "");
        try {
            for (String action : actions) {
                //Get action data
                actionData = getData(action);

                //Action type
                Type type = actionData.type;
                //Replace common placeholders
                String value = actionData.value.replace("{player_name}", player.getName())
                        .replace("{player_uuid}", player.getUniqueId().toString());

                //Replace replacer placeholders
                for (PlayerStringReplacer replacer : stringReplacers)
                    value = replacer.replaceInMessage(player, value);

                //Replace PlaceholderAPI placeholders
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
                    value = PlaceholderAPI.setPlaceholders(player, value);

                //Create split array used by many cases
                String[] split;

                switch (type) {
                    case CONSOLE_COMMAND:
                        //Send console command (without slash)
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', value));
                        break;
                    case COMMAND:
                        //Send player command (without slash)
                        player.chat(ChatColor.translateAlternateColorCodes('&', "/" + value));
                        break;
                    case MESSAGE:
                        //Send a message to the player
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', value));
                        break;
                    case TITLE:
                        //If using server version 1.7, titles are not available
                        if (ClicksPerSecond.SERVER_VERSION.contains("1.7"))
                            continue;

                        //[0] title, [1] subTitle, [2] fadeIn, [3] stay, [4] fadeOut
                        split = split(value);
                        //5 elements required
                        if (split.length != 5)
                            continue;

                        //Separate into different variables
                        String title = ChatColor.translateAlternateColorCodes('&', split[0]),
                                subtitle = ChatColor.translateAlternateColorCodes('&', split[1]);
                        int fadeIn = Integer.parseInt(split[2]), stay = Integer.parseInt(split[3]),
                                fadeOut = Integer.parseInt(split[4]);

                        //If using server version 1.8
                        if (ClicksPerSecond.SERVER_VERSION.contains("1.8")) {
                            //Title packet
                            PacketContainer titlePacket = new PacketContainer(PacketType.Play.Server.TITLE);
                            titlePacket.getTitleActions().write(0, EnumWrappers.TitleAction.TITLE);
                            titlePacket.getChatComponents().write(0, WrappedChatComponent.fromText(title));
                            //Subtitle packet
                            PacketContainer subtitlePacket = new PacketContainer(PacketType.Play.Server.TITLE);
                            subtitlePacket.getTitleActions().write(0, EnumWrappers.TitleAction.SUBTITLE);
                            subtitlePacket.getChatComponents().write(0, WrappedChatComponent.fromText(subtitle));
                            //Timings
                            PacketContainer timingsPacket = new PacketContainer(PacketType.Play.Server.TITLE);
                            timingsPacket.getTitleActions().write(0, EnumWrappers.TitleAction.TIMES);
                            timingsPacket.getIntegers().write(0, fadeIn);
                            timingsPacket.getIntegers().write(1, stay);
                            timingsPacket.getIntegers().write(2, fadeOut);
                            //Send all packets
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, titlePacket);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, subtitlePacket);
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, timingsPacket);
                        } else {
                            //Send title and subtitle
                            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                        }
                        break;
                    case ACTIONBAR:
                        //Create packet
                        PacketContainer actionBarPacket = new PacketContainer(PacketType.Play.Server.CHAT);
                        //Set chat type
                        actionBarPacket.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
                        //Write the message
                        actionBarPacket.getChatComponents().write(0, WrappedChatComponent.fromText(ChatColor.translateAlternateColorCodes('&', value)));
                        //Send the packet to the player
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, actionBarPacket);
                        break;
                    case CHAT:
                        //Chat as the player
                        player.chat(ChatColor.translateAlternateColorCodes('&', value));
                        break;
                    case PARTICLE:
                        //Spawn particle on the player's screen
                        player.spawnParticle(Particle.valueOf(value), player.getLocation(), 1);
                        break;
                    case PARTICLE_PUBLIC:
                        //Spawn particle in the world
                        player.getWorld().spawnParticle(Particle.valueOf(value), player.getLocation(), 1);
                        break;
                    case SOUND:
                    case SOUND_PUBLIC:
                        //[0] sound, [1] volume, [2] speed
                        split = split(value);
                        //3 elements required
                        if (split.length != 3)
                            continue;

                        //Parse speed
                        float speed = Float.parseFloat(split[2]);
                        //Continue if invalid speed
                        if (speed < 0.5F || speed > 2.0F)
                            continue;

                        //If it is public sound
                        if (type.name().contains("PUBLIC"))
                            //Play sound in the world
                            player.getWorld().playSound(player.getLocation(), Sound.valueOf(split[0]), Float.parseFloat(split[1]), speed);
                        else
                            //Play sound to the player
                            player.playSound(player.getLocation(), Sound.valueOf(split[0]), Float.parseFloat(split[1]), speed);
                        break;
                    case EXP_LEVEL:
                        //Set EXP level
                        player.setLevel(Integer.parseInt(value));
                        break;
                    case TELEPORT:
                        //[0] world, [1] X, [2] Y, [3] Z, [4] yaw, [5] pitch
                        split = split(value);
                        //4 or 6 elements required
                        if (split.length != 4 && split.length != 6)
                            continue;

                        //Get the world
                        World world = Bukkit.getWorld(split[0]);
                        //If the world does not exist
                        if (world == null)
                            continue;
                        //Teleport with yaw and pitch or without
                        if (split.length == 4)
                            player.teleport(new Location(world,
                                    Double.parseDouble(split[1]),
                                    Double.parseDouble(split[2]),
                                    Double.parseDouble(split[3])));
                        else
                            player.teleport(new Location(world,
                                    Double.parseDouble(split[1]),
                                    Double.parseDouble(split[2]),
                                    Double.parseDouble(split[3]),
                                    Float.parseFloat(split[4]),
                                    Float.parseFloat(split[5])));
                        break;
                }
            }
        } catch (Exception ex) {
            //Log the error
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error has occurred while running an action (type=" + actionData.type + ", value=" + actionData.value + ")!", ex);
        }
    }

    /**
     * Returns action data from the given action string.
     *
     * @param action the action string to get data from
     * @return the action data
     */
    private ActionData getData(String action) {
        //Get index of [ and ]
        int openBracket = action.indexOf('[');
        int closeBracket = action.indexOf(']');

        Type type;
        String value;

        //If the ] is before [
        if (openBracket >= closeBracket) {
            //Set action to wrong specification
            type = Type.WRONG_ACTION_SPECIFICATION;
        } else {
            //Get action, return if invalid
            type = Type.valueOf(action.substring(openBracket + 1, closeBracket));
        }

        //Get value
        value = action.substring(closeBracket + 1);
        //Return as action data object
        return new ActionData(type, value);
    }

    /**
     * Splits the given string using the {@link #SPLIT_STRING} constant.
     *
     * @param valueOf the string to split
     * @return the split string
     */
    private String[] split(String valueOf) {
        return valueOf.split(SPLIT_STRING);
    }

    /**
     * Class used to store type and value of an action.
     */
    private class ActionData {

        //Type and value
        private Type type;
        private String value;

        /**
         * Initializes the action data.
         *
         * @param type  type of the action
         * @param value value of the action
         */
        private ActionData(Type type, String value) {
            //Set values
            this.type = type;
            this.value = value;
        }

    }
}
