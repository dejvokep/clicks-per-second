package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.ImmediateSampler;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ImmediateHandler implements ClickHandler {

    private final Map<UUID, ImmediateSampler> samplers = new ConcurrentHashMap<>();
    private final ClicksPerSecond plugin;

    public ImmediateHandler(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                while (true)
                    samplers.forEach((player, sampler) -> sampler.clear());
            }
        }.runTask(plugin);
    }

    @Override
    public void add(@NotNull Player player) {
        // Add
        samplers.put(player.getUniqueId(), new ImmediateSampler(PlayerInfo.initial(player.getUniqueId())));
        // Fetch
        plugin.getDataStorage().queueFetch(player.getUniqueId());
    }

    @Override
    public void remove(@NotNull Player player) {
        // If absent
        if (!samplers.containsKey(player.getUniqueId()))
            return;

        // Remove
        PlayerInfo info = samplers.remove(player.getUniqueId()).close();
        // Update
        if (info != null)
            plugin.getDataStorage().update(info);
    }

    @Override
    public void setFetchedInfo(@NotNull PlayerInfo info) {
        Sampler sampler = samplers.get(info.getUniqueId());
        if (sampler != null)
            sampler.setFetchedInfo(info);
    }

    @Override
    public void processClick(@NotNull Player player) {
        // Add click
        PlayerInfo updated = samplers.get(player.getUniqueId()).addClick();
        // No update
        if (updated == null)
            return;

        // Update
        plugin.getDataStorage().update(updated);
    }

    @Override
    @Nullable
    public Sampler getSampler(@NotNull UUID uuid) {
        return samplers.get(uuid);
    }

    @Override
    public int getCPS(@NotNull Player player) {
        // Sampler
        ImmediateSampler sampler = samplers.get(player.getUniqueId());
        // Return
        return sampler != null ? sampler.getCPS() : -1;
    }

    @Override
    @Nullable
    public PlayerInfo getInfo(@NotNull UUID uuid) {
        return samplers.containsKey(uuid) ? samplers.get(uuid).getInfo() : null;
    }

    @Override
    public int getDisplayRate() {
        return 1;
    }

}