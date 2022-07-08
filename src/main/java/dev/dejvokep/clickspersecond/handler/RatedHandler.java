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
    private final ClicksPerSecond plugin;

    private final int rate;

    public RatedHandler(ClicksPerSecond plugin, int rate) {
        this.plugin = plugin;
        this.rate = rate;

        // Schedule
        Bukkit.getScheduler().runTaskTimer(plugin, () -> samplers.forEach((uuid, sampler) -> {
            // Reset
            PlayerInfo updated = sampler.reset();
            // Update
            if (updated != null)
                plugin.getDataStorage().update(updated);
        }), rate, rate);
    }

    @Override
    public void add(Player player) {
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
        samplers.get(player.getUniqueId()).addClick();
    }

    @Override
    public Sampler getSampler(UUID uuid) {
        return samplers.get(uuid);
    }

    @Override
    public int getCPS(Player player) {
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