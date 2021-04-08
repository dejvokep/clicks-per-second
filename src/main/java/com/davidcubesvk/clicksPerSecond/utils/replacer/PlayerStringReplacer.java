package com.davidcubesvk.clicksPerSecond.utils.replacer;

import org.bukkit.entity.Player;

/**
 * Interface used to replace placeholders in strings.
 */
public interface PlayerStringReplacer {

    /**
     * Replaces placeholders in the given message.
     *
     * @param player  the receiver of the message
     * @param message the message to replace placeholders in
     * @return the message with replaced placeholders
     */
    String replaceInMessage(Player player, String message);

}
