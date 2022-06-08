package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.ImmediateSampler;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ImmediateHandler implements ClickHandler {

    private final Map<UUID, ImmediateSampler> samplers = new ConcurrentHashMap<>();
    private BukkitTask task;
    private final ClicksPerSecond plugin;

    private boolean running = false;

    public ImmediateHandler(ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(int rate) {
        // If running
        if (running)
            return;

        // Running
        running = true;
        // Schedule
        task = new BukkitRunnable() {
            @Override
            public void run() {
                while (running)
                    samplers.forEach((player, sampler) -> sampler.clear());
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void stop() {
        // If not running
        if (!running)
            return;

        // Cancel
        task.cancel();
        task = null;
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
        samplers.put(player.getUniqueId(), new ImmediateSampler(PlayerInfo.initial(player.getUniqueId())));
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
        PlayerInfo updated = samplers.get(player.getUniqueId()).addClick();
        // No update
        if (updated == null)
            return;

        // Update
        plugin.getDataStorage().update(updated);
    }

    @Override
    public int getCPS(Player player) {
        // If not running
        if (!running)
            return -1;

        // Sampler
        ImmediateSampler sampler = samplers.get(player.getUniqueId());
        // Return
        return sampler != null ? sampler.getCPS() : -1;
    }

    @Override
    public PlayerInfo getInfo(UUID uuid) {
        return samplers.containsKey(uuid) ? samplers.get(uuid).getInfo() : null;
    }

    @Override
    public int getDisplayRate() {
        return 1;
    }

}