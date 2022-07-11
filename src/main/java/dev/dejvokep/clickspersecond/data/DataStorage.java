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
package dev.dejvokep.clickspersecond.data;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract class for data storages.
 */
public abstract class DataStorage {

    // Plugin
    private final ClicksPerSecond plugin;

    // Tasks
    private BukkitTask syncTask, leaderboardTask;
    // Sync queue
    private Set<PlayerInfo> sync = new HashSet<>();

    // Leaderboard
    private List<PlayerInfo> leaderboard = Collections.emptyList();
    private int leaderboardLimit;
    private long leaderboardExpiration;

    // If ready
    private boolean ready = false;

    /**
     * Initializes the data storage.
     *
     * @param plugin the plugin
     * @param type   type of the storage (to log)
     */
    public DataStorage(@NotNull ClicksPerSecond plugin, @NotNull String type) {
        this.plugin = plugin;
        plugin.getLogger().info("Using " + type + " to save CPS data.");
    }

    /**
     * Reloads the internal configuration.
     */
    public void reload() {
        // Cancel
        if (syncTask != null)
            syncTask.cancel();
        if (leaderboardTask != null)
            leaderboardTask.cancel();

        // Set
        this.leaderboardLimit = Math.max(plugin.getConfiguration().getInt("data.leaderboard.limit"), 1);
        this.leaderboardExpiration = Math.max(plugin.getConfiguration().getLong("data.leaderboard.expiration"), 1L);

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

        // Fetch leaderboard
        fetchBoard();
    }

    /**
     * Passes the given fetched info to the appropriate sampler.
     *
     * @param info the info to pass
     */
    protected void passToSampler(@NotNull PlayerInfo info) {
        plugin.getClickHandler().setFetchedInfo(info);
    }

    /**
     * Queues the given info for sync.
     *
     * @param info the info to queue
     */
    public void sync(@NotNull PlayerInfo info) {
        sync.remove(info);
        sync.add(info);
    }

    /**
     * Indicates that the data storage is ready to process data requests.
     */
    protected void ready() {
        // Ready
        if (ready)
            return;

        // Ready
        ready = true;
        // Reload
        reload();
    }

    /**
     * Fetches and caches the leaderboard with the configured fetch limit.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @return the fetched leaderboard
     * @see #fetchBoard()
     */
    public CompletableFuture<List<PlayerInfo>> fetchBoard() {
        return fetchBoard(leaderboardLimit);
    }

    /**
     * Fetches and caches the leaderboard with the provided fetch limit.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @param limit max places to retrieve
     * @return the fetched leaderboard
     */
    public CompletableFuture<List<PlayerInfo>> fetchBoard(int limit) {
        // Fetch
        CompletableFuture<List<PlayerInfo>> board = fetchLeaderboard(limit);
        // Run internal logic
        board.whenComplete((data, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            leaderboard = data == null ? Collections.emptyList() : data;
            leaderboardTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Not ready
                if (!ready)
                    return;

                fetchBoard();
            }, leaderboardExpiration);
        }));

        // Return
        return board;
    }

    /**
     * Returns the leaderboard fetch limit (max places to retrieve). This limit (or any other) is ignored by the {@link
     * FileStorage}, which always caches all data.
     *
     * @return the leaderboard fetch limit
     */
    public int getLeaderboardLimit() {
        return leaderboardLimit;
    }

    /**
     * Closes the data storage and its connections.
     */
    public abstract void close();

    /**
     * Returns if the storage has instant fetching.
     *
     * @return if the storage has instant fetching
     */
    public abstract boolean isInstantFetch();

    /**
     * Queues the given {@link UUID unique ID} for fetching.
     *
     * @param uuid the ID to queue
     */
    public abstract void queueFetch(@NotNull UUID uuid);

    /**
     * Skips fetching data for the given {@link UUID unique ID}. More formally, if still in the fetch queue, removes
     * it.
     *
     * @param uuid the ID to skip
     */
    public abstract void skipFetch(@NotNull UUID uuid);

    /**
     * Fetches and returns a single info.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @param uuid      the ID to fetch for
     * @param skipCache if to skip cache checking, if any
     * @return the fetched info
     */
    @NotNull
    public abstract CompletableFuture<PlayerInfo> fetchSingle(@NotNull UUID uuid, boolean skipCache);

    /**
     * Deletes a single info. Returns if the operation was successful.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @param uuid the ID to delete
     * @return if the operation was successful
     */
    @NotNull
    public abstract CompletableFuture<Boolean> delete(@NotNull UUID uuid);

    /**
     * Deletes all data. Returns if the operation was successful.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @return if the operation was successful
     */
    @NotNull
    public abstract CompletableFuture<Boolean> deleteAll();

    /**
     * Fetches the leaderboard with the provided fetch limit (ignored by {@link FileStorage}). Caching is not handled by
     * the method.
     * <p>
     * <i>The returned future will not contain any exceptions under normal circumstances, so it is redundant to process
     * them.</i>
     *
     * @param limit max places to retrieve
     * @return the fetched leaderboard
     */
    @NotNull
    protected abstract CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit);

    /**
     * Syncs the given queued info.
     *
     * @param queued the queued info to sync
     */
    protected abstract void sync(@NotNull Set<PlayerInfo> queued);

    /**
     * Returns the cached leaderboard.
     *
     * @return the cached leaderboard
     */
    @NotNull
    public List<PlayerInfo> getLeaderboard() {
        return leaderboard;
    }

    /**
     * Returns the plugin.
     *
     * @return the plugin
     */
    @NotNull
    public ClicksPerSecond getPlugin() {
        return plugin;
    }

}