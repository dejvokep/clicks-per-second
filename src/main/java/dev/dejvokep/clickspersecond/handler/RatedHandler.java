package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.RatedSampler;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RatedHandler implements ClickHandler {

    private final Map<UUID, RatedSampler> samplers = new HashMap<>();
    private BukkitTask task;
    private final ClicksPerSecond plugin;

    private boolean running = false;
    private int rate;

    public RatedHandler(ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(int rate) {
        // If running
        if (running)
            return;

        // Set
        this.rate = rate;
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> samplers.forEach((uuid, sampler) -> {
            // Reset
            PlayerInfo updated = sampler.reset();
            // Update
            if (updated != null)
                plugin.getDataStorage().update(updated);
        }), rate, rate);
        // Running
        running = true;
    }

    @Override
    public void stop() {
        // If not running
        if (!running)
            return;

        // Cancel
        task.cancel();
        // Not running
        running = false;
        // Clear
        samplers.clear();
    }

    @Override
    public void add(Player player) {
        // If not running
        if (!running)
            return;
        // Add
        samplers.put(player.getUniqueId(), new RatedSampler(rate, PlayerInfo.initial(player.getUniqueId())));
        // Fetch
        plugin.getDataStorage().queueFetch(player.getUniqueId());
    }

    @Override
    public void remove(Player player) {
        // Remove
        PlayerInfo info = samplers.remove(player.getUniqueId()).close();
        // Update
        if (info != null)
            plugin.getDataStorage().update(info);
    }

    @Override
    public void removeAll() {
        samplers.forEach((uuid, sampler) -> remove(Objects.requireNonNull(Bukkit.getPlayer(uuid))));
    }

    @Override
    public void setFetchedInfo(PlayerInfo info) {
        Sampler sampler = samplers.get(info.getUniqueId());
        if (sampler != null)
            sampler.setFetchedInfo(info);
    }

    @Override
    public void processClick(Player player) {
        // If not running
        if (!running)
            return;

        // Add click
        samplers.get(player.getUniqueId()).addClick();
    }

    @Override
    public int getCPS(Player player) {
        // If not running
        if (!running)
            return -1;

        // Sampler
        RatedSampler sampler = samplers.get(player.getUniqueId());
        // Return
        return sampler != null ? sampler.getCPS() : -1;
    }

    @Override
    public PlayerInfo getInfo(UUID uuid) {
        return samplers.containsKey(uuid) ? samplers.get(uuid).getInfo() : null;
    }

    @Override
    public int getDisplayRate() {
        return rate;
    }

}