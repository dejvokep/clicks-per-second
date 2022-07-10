package dev.dejvokep.clickspersecond.data;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class DataStorage {

    private final ClicksPerSecond plugin;

    private BukkitTask syncTask, leaderboardTask;
    private Set<PlayerInfo> sync = new HashSet<>();

    private List<PlayerInfo> leaderboard = Collections.emptyList();
    private int leaderboardLimit;

    private boolean ready = false;

    public DataStorage(@NotNull ClicksPerSecond plugin, @NotNull String type) {
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

            fetchBoard(leaderboardLimit);
        }, 0L, Math.max(plugin.getConfiguration().getLong("data.leaderboard.expiration"), 1L));
    }

    protected void passToSampler(@NotNull PlayerInfo info) {
        plugin.getClickHandler().setFetchedInfo(info);
    }

    public void update(@NotNull PlayerInfo info) {
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

    public CompletableFuture<List<PlayerInfo>> fetchBoard() {
        return fetchBoard(leaderboardLimit);
    }

    public CompletableFuture<List<PlayerInfo>> fetchBoard(int limit) {
        // Fetch
        CompletableFuture<List<PlayerInfo>> board = fetchLeaderboard(limit);
        // Run internal logic
        board.whenComplete((data, ex) -> Bukkit.getScheduler().runTask(plugin, () -> leaderboard = data == null ? Collections.emptyList() : data));

        // Return
        return board;
    }

    public int getLeaderboardLimit() {
        return leaderboardLimit;
    }

    public abstract void close();

    public abstract boolean isInstantFetch();

    public abstract void queueFetch(@NotNull UUID uuid);

    public abstract void skipFetch(@NotNull UUID uuid);

    @NotNull
    public abstract CompletableFuture<PlayerInfo> fetchSingle(@NotNull UUID uuid);

    @NotNull
    public abstract CompletableFuture<Boolean> delete(@NotNull UUID uuid);

    @NotNull
    public abstract CompletableFuture<Boolean> deleteAll();

    @NotNull
    protected abstract CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit);

    protected abstract void sync(@NotNull Set<PlayerInfo> queued);

    @NotNull
    public List<PlayerInfo> getLeaderboard() {
        return leaderboard;
    }

    @NotNull
    public ClicksPerSecond getPlugin() {
        return plugin;
    }

}