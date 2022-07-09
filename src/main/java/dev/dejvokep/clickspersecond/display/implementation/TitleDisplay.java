package dev.dejvokep.clickspersecond.display.implementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.watcher.VariableMessage;
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

public class TitleDisplay implements Display {

    private static final boolean USE_PACKETS = Bukkit.getBukkitVersion().contains("1.8");

    private final Set<Player> players = new HashSet<>();

    private final ClicksPerSecond plugin;

    private BukkitTask task;
    private VariableMessage<TitleMessages> message;
    private int refresh;

    public TitleDisplay(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(@NotNull Player player) {
        // If the task is not running
        if (task == null)
            return;
        // Add
        players.add(player);
    }

    @Override
    public void remove(@NotNull Player player) {
        // If the task is not running
        if (task == null)
            return;

        // Remove
        if (players.remove(player))
            clear(player);
    }

    @Override
    public void removeAll() {
        players.forEach(this::remove);
    }

    @Override
    public void reload() {
        // Config
        Section config = plugin.getConfiguration().getSection("display.title");

        // Cancel
        if (task != null) {
            task.cancel();
            task = null;
        }

        // If disabled
        if (!config.getBoolean("enabled") || (USE_PACKETS && !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")))
            return;

        // Set
        message = VariableMessage.of(plugin, new TitleMessages(config.getSection("normal")), new TitleMessages(config.getSection("watching")));
        refresh = Math.max(config.getInt("refresh"), plugin.getClickHandler().getDisplayRate());
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> players.forEach(this::send), 0L, refresh);
    }

    @SuppressWarnings("deprecation")
    public void send(@NotNull Player player) {
        // Replace
        TitleMessages messages = message.get(player);
        String title = messages.getTitle(player);
        String subtitle = messages.getSubtitle(player);

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

    @SuppressWarnings("deprecation")
    public void clear(@NotNull Player player) {
        player.sendTitle("", "");
    }

    private class TitleMessages {
        private final String title, subtitle;

        private TitleMessages(@NotNull Section config) {
            title = ChatColor.translateAlternateColorCodes('&', config.getString("title"));
            subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("subtitle"));
        }

        @NotNull
        public String getTitle(@NotNull Player player) {
            return plugin.getPlaceholderReplacer().all(player, title);
        }

        @NotNull
        public String getSubtitle(@NotNull Player player) {
            return plugin.getPlaceholderReplacer().all(player, subtitle);
        }
    }

}