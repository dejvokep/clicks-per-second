package dev.dejvokep.clickspersecond.utils.container;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A simple player container interface.
 */
public interface PlayerContainer {

    /**
     * Adds the given player to the container.
     * @param player the player to add
     */
    void add(@NotNull Player player);

    /**
     * Removes the given player from the container.
     * @param player the player to remove
     */
    void remove(@NotNull Player player);

}