package dev.dejvokep.clickspersecond.display.implementation;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import dev.dejvokep.clickspersecond.utils.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

public class BossBarDisplay implements Display {

    private static final boolean BOSS_BAR_UNAVAILABLE = Bukkit.getBukkitVersion().contains("1.7") || Bukkit.getBukkitVersion().contains("1.8");

    private final ClicksPerSecond plugin;

    private final Map<Player, BossBar> bossBars = new HashMap<>();

    private BukkitTask task;

    private BarColor color;
    private BarStyle style;
    private BarFlag[] flags;
    private double progress;
    private String message;

    public BossBarDisplay(ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(Player player) {
        // If the task is not running
        if (task == null)
            return;

        // Create bar
        BossBar bossBar = Bukkit.createBossBar(Placeholders.set(player, message), color, style, flags);
        // Set progress
        bossBar.setProgress(progress);
        // Add
        bossBar.addPlayer(player);
        bossBars.put(player, bossBar);
    }

    @Override
    public void remove(Player player) {
        // If the task is not running
        if (task == null)
            return;

        // Boss bar
        BossBar bossBar = bossBars.get(player);
        // If null
        if (bossBar == null)
            return;

        // Remove
        bossBar.removeAll();
        bossBars.remove(player);
    }

    @Override
    public void removeAll() {
        bossBars.keySet().forEach(this::remove);
    }

    public void reload() {
        // Config
        Section config = plugin.getConfiguration().getSection("display.boss-bar");

        // Cancel
        if (task != null) {
            task.cancel();
            task = null;
        }

        // If disabled
        if (!config.getBoolean("enabled") || BOSS_BAR_UNAVAILABLE)
            return;

        // Set
        message = ChatColor.translateAlternateColorCodes('&', config.getString("message"));
        color = map(() -> BarColor.valueOf(config.getString("color").toUpperCase()), BarColor.WHITE, "Boss bar color is invalid!");
        style = map(() -> BarStyle.valueOf(config.getString("style").toUpperCase()), BarStyle.SOLID, "Boss bar style is invalid!");
        flags = config.getStringList("flags").stream().map(flag -> map(() -> BarFlag.valueOf(flag.toUpperCase()), null, "Bar flag is invalid!")).filter(Objects::nonNull).toArray(BarFlag[]::new);
        progress = clamp(config.getDouble("progress"), 0, 1);
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> bossBars.forEach((player, bossBar) -> bossBar.setTitle(Placeholders.set(player, message).replace("{cps}", String.valueOf(plugin.getClickHandler().getCPS(player))))), 0L, Math.max(config.getInt("refresh"), plugin.getClickHandler().getDisplayRate()));
    }

    private <T> T map(Supplier<T> supplier, T def, String message) {
        // Try
        try {
            return supplier.get();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, message, ex);
        }

        // Return
        return def;
    }

    private double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

}