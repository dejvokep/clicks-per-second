package dev.dejvokep.clickspersecond.utils.watcher;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * Class responsible for handling variable messages between while in normal gameplay and when in watching mode.
 *
 * @param <T> type of the messages
 */
public class VariableMessages<T> {

    // Manager
    private final WatchManager watchManager;
    // Messages
    private final T normal, watching;

    /**
     * Initializes the variable message.
     *
     * @param plugin   the plugin
     * @param normal   the normal message
     * @param watching the message in watching mode
     */
    private VariableMessages(@NotNull ClicksPerSecond plugin, @NotNull T normal, @NotNull T watching) {
        this.watchManager = plugin.getWatchManager();
        this.normal = normal;
        this.watching = watching;
    }

    /**
     * Returns the appropriate message depending on the player's mode.
     *
     * @param player the player to return for
     * @return the message
     * @see #get(Player, BiFunction)
     */
    @NotNull
    public T get(@NotNull Player player) {
        return get(player, (message, target) -> message);
    }

    /**
     * Maps and returns the appropriate message depending on the player's mode.
     *
     * @param player the player to return for
     * @param mapper mapper to apply to the original message (of type {@link T})
     * @param <R>    type of the mapped object
     * @return the mapped message
     * @see #get(Player, BiFunction)
     */
    @NotNull
    public <R> R get(@NotNull Player player, @NotNull BiFunction<T, Player, R> mapper) {
        Player watched = watchManager.getWatched(player);
        T message = watched == null ? normal : watching;
        return mapper.apply(message, watched == null ? player : watched);
    }

    /**
     * Initializes from the given information.
     *
     * @param plugin   the plugin
     * @param normal   normal message
     * @param watching message when in watching mode
     * @param <T>      type of the messages
     * @return the instance
     */
    @NotNull
    public static <T> VariableMessages<T> of(@NotNull ClicksPerSecond plugin, @NotNull T normal, @NotNull T watching) {
        return new VariableMessages<>(plugin, normal, watching);
    }

    /**
     * Initializes from the given information. The given section must contain keys <code>normal</code> and
     * <code>watching</code>; color codes will automatically be translated.
     *
     * @param plugin  the plugin
     * @param section the section to obtain the messages from
     * @return the instance
     */
    @NotNull
    public static VariableMessages<String> of(@NotNull ClicksPerSecond plugin, @NotNull Section section) {
        return new VariableMessages<>(plugin, ChatColor.translateAlternateColorCodes('&', section.getString("normal")), ChatColor.translateAlternateColorCodes('&', section.getString("watching")));
    }

}