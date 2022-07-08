package dev.dejvokep.clickspersecond.display.implementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class TitleDisplay implements Display {

    private static final boolean TITLES_SUPPORTED = !Bukkit.getBukkitVersion().contains("1.7");
    private static final boolean USE_PACKETS = Bukkit.getBukkitVersion().contains("1.8");

    private final Set<Player> players = new HashSet<>();

    private final ClicksPerSecond plugin;

    private BukkitTask task;
    private String title, subtitle;
    private int refresh;

    public TitleDisplay(ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(Player player) {
        // If the task is not running
        if (task == null)
            return;
        // Add
        players.add(player);
    }

    @Override
    public void remove(Player player) {
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
        if (!config.getBoolean("enabled") || !TITLES_SUPPORTED)
            return;

        // Set
        title = ChatColor.translateAlternateColorCodes('&', config.getString("title"));
        subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("subtitle"));
        refresh = Math.max(config.getInt("refresh"), plugin.getClickHandler().getDisplayRate());
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> players.forEach(this::send), 0L, refresh);
    }

    @SuppressWarnings("deprecated")
    public void send(Player player) {
        // Replace
        Sampler sampler = plugin.getClickHandler().getSampler(player.getUniqueId());
        String title = plugin.getPlaceholderReplacer().replace(sampler, this.title);
        String subtitle = plugin.getPlaceholderReplacer().replace(sampler, this.subtitle);

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

    @SuppressWarnings("deprecated")
    public void clear(Player player) {
        player.sendTitle("", "");
    }

}