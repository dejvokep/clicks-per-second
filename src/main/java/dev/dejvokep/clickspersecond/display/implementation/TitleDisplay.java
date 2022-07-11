package dev.dejvokep.clickspersecond.display.implementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.watcher.VariableMessages;
import dev.dejvokep.clickspersecond.display.Display;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Title display.
 */
public class TitleDisplay implements Display {

    /**
     * Indicates if to use packets to send titles instead of the server API.
     */
    private static final boolean USE_PACKETS = Bukkit.getBukkitVersion().contains("1.8");

    private final Set<Player> players = new HashSet<>();
    private final ClicksPerSecond plugin;

    private BukkitTask task;
    private VariableMessages<TitleMessages> message;
    private int refresh;

    /**
     * Initializes the display. Automatically calls {@link #reload()}.
     *
     * @param plugin the plugin
     */
    public TitleDisplay(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(@NotNull Player player) {
        players.add(player);
    }

    @Override
    public void remove(@NotNull Player player) {
        if (players.remove(player))
            clear(player);
    }

    @Override
    public void reload() {
        // Config
        Section config = plugin.getConfiguration().getSection("display.title");

        // Cancel
        if (task != null)
            task.cancel();

        // If disabled
        if (!config.getBoolean("enabled") || (USE_PACKETS && !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")))
            return;

        // Set
        message = VariableMessages.of(plugin, new TitleMessages(config.getSection("normal")), new TitleMessages(config.getSection("watching")));
        refresh = Math.max(config.getInt("refresh"), plugin.getClickHandler().getMinDisplayRate());
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> players.forEach(this::send), 0L, refresh);
    }

    /**
     * Sends the configured title to the given player.
     *
     * @param player the player to send to
     */
    public void send(@NotNull Player player) {
        // Messages
        TitleMessages messages = message.get(player);
        String title = messages.getTitle(player);
        String subtitle = messages.getSubtitle(player);
        // Send
        send(player, title, subtitle);
    }

    /**
     * Sends the given title to the given player.
     *
     * @param player   the player to send to
     * @param title    the title to send
     * @param subtitle the subtitle to send
     */
    @SuppressWarnings("deprecation")
    public void send(@NotNull Player player, @NotNull String title, @NotNull String subtitle) {
        // If to not use packets
        if (!USE_PACKETS) {
            // Send
            player.sendTitle(title, subtitle, 0, refresh + 20, 0);
            return;
        }

        try {
            // Timings
            PacketContainer timings = new PacketContainer(PacketType.Play.Server.TITLE);
            timings.getTitleActions().write(0, EnumWrappers.TitleAction.TIMES);
            timings.getIntegers().write(0, 0);
            timings.getIntegers().write(1, refresh + 20);
            timings.getIntegers().write(2, 0);
            // Send timings
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, timings);
            // Send title via the API
            player.sendTitle(title, subtitle);
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send title!", ex);
        }
    }

    /**
     * Clears title for the specified player
     *
     * @param player the player to clear for
     */
    public void clear(@NotNull Player player) {
        send(player, "", "");
    }

    /**
     * Class used to store title messages (title + subtitle).
     */
    private class TitleMessages {
        private final String title, subtitle;

        /**
         * Initializes the title messages from the given section. The section must contain <code>title</code> and
         * <code>subtitle</code>; color codes will automatically be translated.
         *
         * @param section the section to obtain the messages from
         */
        private TitleMessages(@NotNull Section section) {
            title = ChatColor.translateAlternateColorCodes('&', section.getString("title"));
            subtitle = ChatColor.translateAlternateColorCodes('&', section.getString("subtitle"));
        }

        /**
         * Returns the title to show to the given player. Placeholders are automatically translated.
         *
         * @param player the player to return for
         * @return the title to show to the player
         */
        @NotNull
        public String getTitle(@NotNull Player player) {
            return plugin.getPlaceholderReplacer().api(player, title);
        }

        /**
         * Returns the subtitle to show to the given player. Placeholders are automatically translated.
         *
         * @param player the player to return for
         * @return the subtitle to show to the player
         */
        @NotNull
        public String getSubtitle(@NotNull Player player) {
            return plugin.getPlaceholderReplacer().api(player, subtitle);
        }
    }

}