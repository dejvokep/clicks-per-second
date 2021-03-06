/*
 * Copyright 2022 https://dejvokep.dev/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dejvokep.clickspersecond.display.implementation;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.watcher.VariableMessages;
import dev.dejvokep.clickspersecond.display.Display;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Boss bar display.
 */
public class BossBarDisplay implements Display {

    /**
     * Indicates whether the boss bar feature is unavailable.
     */
    private static final boolean BOSS_BAR_UNAVAILABLE = Bukkit.getBukkitVersion().contains("1.8");

    // Players
    private final Map<Player, BossBar> bossBars = new HashMap<>();

    // Internals
    private final ClicksPerSecond plugin;
    private BukkitTask task;
    private VariableMessages<String> message;

    // Properties
    private BarColor color;
    private BarStyle style;
    private BarFlag[] flags;
    private double progress;

    /**
     * Initializes the display. Automatically calls {@link #reload()}.
     *
     * @param plugin the plugin
     */
    public BossBarDisplay(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(@NotNull Player player) {
        // If the task is not running
        if (task == null)
            return;

        // Create bar
        BossBar bossBar = Bukkit.createBossBar(message.get(player), color, style, flags);
        // Set progress
        bossBar.setProgress(progress);
        // Add
        bossBar.addPlayer(player);
        bossBars.put(player, bossBar);
    }

    @Override
    public void remove(@NotNull Player player) {
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
        message = VariableMessages.of(plugin, config.getSection("message"));
        color = get(() -> BarColor.valueOf(config.getString("color").toUpperCase()), BarColor.WHITE, "Boss bar color is invalid!");
        style = get(() -> BarStyle.valueOf(config.getString("style").toUpperCase()), BarStyle.SOLID, "Boss bar style is invalid!");
        flags = config.getStringList("flags").stream().map(flag -> get(() -> BarFlag.valueOf(flag.toUpperCase()), null, "Bar flag is invalid!")).filter(Objects::nonNull).toArray(BarFlag[]::new);
        progress = clamp(config.getDouble("progress"), 0, 1);
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> bossBars.forEach((player, bossBar) -> bossBar.setTitle(message.get(player, (message, target) -> plugin.getPlaceholderReplacer().api(target, message)))), 0L, Math.max(config.getInt("refresh"), plugin.getClickHandler().getMinDisplayRate()));
    }

    /**
     * Returns the value from the supplier, or the provided default if an {@link Exception exception} is thrown. In such
     * case it is logged with the given message.
     *
     * @param supplier supplier of the value
     * @param def      default value
     * @param message  error message
     * @param <T>      type of the value
     * @return the value, or default as described above
     */
    @Nullable
    private <T> T get(@NotNull Supplier<T> supplier, @Nullable T def, @NotNull String message) {
        // Try
        try {
            return supplier.get();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, message, ex);
        }

        // Return
        return def;
    }

    /**
     * Clamps the value between the provided boundaries. More formally, if:
     * <ul>
     *     <li><code>value < min</code>, returns <code>min</code>,</li>
     *     <li><code>min <= value <= max</code>, returns <code>value</code>,</li>
     *     <li><code>value > max</code>, returns <code>max</code>.</li>
     * </ul>
     *
     * @param value the value to clamp
     * @param min   the min boundary
     * @param max   the max boundary
     * @return the clamped value
     */
    private double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

}