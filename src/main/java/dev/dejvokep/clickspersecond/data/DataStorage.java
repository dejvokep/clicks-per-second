package dev.dejvokep.clickspersecond.data;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class DataStorage {

    private final ClicksPerSecond plugin;

    private BukkitTask syncTask, leaderboardTask;
    private Set<PlayerInfo> sync = new HashSet<>();

    private List<PlayerInfo> leaderboard = Collections.emptyList();
    private int leaderboardLimit;

    private boolean ready = false;

    public DataStorage(ClicksPerSecond plugin, String type) {
        // Set
        this.plugin = plugin;
        // Log
        plugin.getLogger().info("Using " + type + " to save CPS data.");
    }

    public void reload() {
        // Cancel
        if (syncTask != null)
            syncTask.cancel();
        if (leaderboardTask != null)
            leaderboardTask.cancel();

        // Set
        this.leaderboardLimit = Math.max(plugin.getConfiguration().getInt("data.leaderboard.limit"), 1);

        // Schedule sync task
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // If not any
            if (sync.isEmpty() || !ready)
                return;

            // Queued
            Set<PlayerInfo> queued = sync;
            // Clear
            sync = new HashSet<>();

            // Sync
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sync(queued));
        }, 0L, Math.max(plugin.getConfiguration().getInt("data.sync-rate"), 1));

        // Schedule leaderboard task
        leaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Not ready
            if (!ready)
                return;

            // Fetch
            fetchLeaderboard(leaderboardLimit).whenComplete((data, ex) -> {
                // An error occurred
                if (ex != null)
                    plugin.getLogger().log(Level.SEVERE, "Failed to fetch leaderboard data!", ex);
                else
                    Bukkit.getScheduler().runTask(plugin, () -> leaderboard = data);
            });
        }, 0L, Math.max(plugin.getConfiguration().getLong("data.leaderboard.expiration"), 1L));
    }

    protected void cache(PlayerInfo info) {
        plugin.getClickHandler().setFetchedInfo(info);
    }

    public void update(PlayerInfo info) {
        sync.remove(info);
        sync.add(info);
    }

    protected void ready() {
        // Ready
        if (ready)
            return;

        // Ready
        ready = true;
        // Reload
        reload();
    }

    public abstract void queueFetch(UUID uuid);

    public abstract void skipFetch(UUID uuid);

    public abstract CompletableFuture<PlayerInfo> fetchSingle(UUID uuid);

    public abstract CompletableFuture<Boolean> delete(UUID uuid);

    public abstract CompletableFuture<Boolean> deleteAll();

    protected abstract CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit);

    protected abstract void sync(Set<PlayerInfo> queued);

    public List<PlayerInfo> getLeaderboard() {
        return leaderboard;
    }

    public ClicksPerSecond getPlugin() {
        return plugin;
    }

}