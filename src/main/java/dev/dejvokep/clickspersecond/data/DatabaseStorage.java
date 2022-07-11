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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Implementation of {@link DataStorage} for databases (MySQL).
 */
public class DatabaseStorage extends DataStorage {

    /**
     * Statement for creating the database table.
     */
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s(uuid CHAR(36), cps INT, t BIGINT(20) UNSIGNED, toggle BOOLEAN, PRIMARY KEY(uuid))";
    /**
     * Statement for deleting all data.
     */
    private static final String SQL_DELETE_ALL = "DELETE FROM %s";
    /**
     * Statement for deleting specific data.
     */
    private static final String SQL_DELETE = "DELETE FROM %s WHERE uuid=?";
    /**
     * Statement for syncing data.
     */
    private static final String SQL_SYNC = "INSERT INTO %s(uuid, cps, t, toggle) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE cps = CASE WHEN cps < VALUES(cps) THEN VALUES(cps) ELSE cps END, t = CASE WHEN cps < VALUES(cps) THEN VALUES(t) ELSE t END, toggle = VALUES(toggle)";
    /**
     * Statement for fetching specific data.
     */
    private static final String SQL_FETCH = "SELECT * FROM %s WHERE uuid=?";
    /**
     * Statement for fetching all (multiple) data.
     */
    private static final String SQL_FETCH_ALL = "SELECT * FROM %s WHERE uuid IN (%s)";
    /**
     * Statement for fetching limited leaderboard data.
     */
    private static final String SQL_LEADERBOARD_LIMITED = "SELECT * FROM %s ORDER BY cps DESC LIMIT ?";
    /**
     * Statement for fetching unlimited leaderboard data.
     */
    private static final String SQL_LEADERBOARD_UNLIMITED = "SELECT * FROM %s ORDER BY cps DESC";
    /**
     * Cache clear delay in ticks.
     */
    private static final long CACHE_CLEAR_DELAY = 20L;

    // Data source
    private final HikariDataSource dataSource;
    private final String table;

    // Fetching
    private final long fetchExpiration, fetchRate;
    private final int fetchSize;

    // Caching
    private final Map<UUID, PlayerInfo> cache = new HashMap<>();
    private final Queue<PlayerInfo> expirationQueue = new LinkedList<>();

    // Fetch queue
    private Set<UUID> fetch = new HashSet<>();

    /**
     * Initializes the data storage.
     *
     * @param plugin the plugin
     */
    public DatabaseStorage(@NotNull ClicksPerSecond plugin) {
        // Call
        super(plugin, "database");
        // Configure
        Section section = plugin.getConfiguration().getSection("database");
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?characterEncoding=%s", section.getString("host"), section.getInt("port"), section.getString("database"), section.getString("encoding")));
        config.setUsername(section.getString("username"));
        config.setPassword(section.getString("password"));
        config.setMaximumPoolSize(section.getInt("max-pool-size"));
        config.setConnectionTimeout(section.getInt("connection-timeout.request"));
        config.setKeepaliveTime(section.getInt("connection-timeout.keep-alive"));
        config.setMaxLifetime(section.getInt("connection-timeout.lifetime"));
        // Properties
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("maintainTimeStats", false);
        // Set
        this.table = section.getString("table");
        this.fetchExpiration = plugin.getConfiguration().getLong("data.fetch.expiration");
        this.fetchSize = plugin.getConfiguration().getInt("data.fetch.batch.size");
        this.fetchRate = plugin.getConfiguration().getLong("data.fetch.batch.rate");
        // Create
        dataSource = new HikariDataSource(config);

        // Run immediately async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_CREATE_TABLE, table))) {
                // Execute
                statement.executeUpdate();
                // Ready
                ready();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create the database table!", ex);
            }
        });

        // Run cache clear task
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // While expired
            long time = System.currentTimeMillis();
            while (expirationQueue.size() > 0 && expirationQueue.peek().getFetchTime() + fetchExpiration < time)
                cache.remove(expirationQueue.remove().getUniqueId());
        }, CACHE_CLEAR_DELAY, CACHE_CLEAR_DELAY);

        // Schedule fetch task
        if (fetchRate > 0)
            Bukkit.getScheduler().runTaskTimer(plugin, this::fetchAll, 0L, fetchRate);
    }

    @Override
    public void sync(@NotNull Set<PlayerInfo> queued) {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_SYNC, table))) {
                // For each
                for (PlayerInfo info : queued) {
                    // Set
                    statement.setString(1, info.getUniqueId().toString());
                    statement.setInt(2, info.getCPS());
                    statement.setLong(3, info.getTime());
                    statement.setBoolean(4, info.getToggle());
                    // Add
                    statement.addBatch();
                }
                // Execute all
                statement.executeBatch();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to execute a batch statement!", ex);
            }
        });
    }

    @Override
    public void queueFetch(@NotNull UUID uuid) {
        // If cached
        if (cache.containsKey(uuid)) {
            passToSampler(cache.get(uuid));
            return;
        }

        // Queue
        fetch.add(uuid);

        // If to fetch immediately or if passed the trigger
        if (fetchRate <= 0 || fetch.size() >= fetchSize)
            fetchAll();
    }

    @Override
    public void skipFetch(@NotNull UUID uuid) {
        fetch.remove(uuid);
    }

    @Override
    @NotNull
    public CompletableFuture<PlayerInfo> fetchSingle(@NotNull UUID uuid) {
        // If cached
        if (cache.containsKey(uuid))
            return CompletableFuture.completedFuture(cache.get(uuid));

        // Supply
        return CompletableFuture.supplyAsync(() -> {
            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_FETCH, table))) {
                // Set
                statement.setString(1, uuid.toString());
                // Execute
                ResultSet resultSet = statement.executeQuery();

                // Info
                PlayerInfo info = resultSet.next() ? PlayerInfo.from(uuid, resultSet.getInt(2), resultSet.getLong(3)) : PlayerInfo.empty(uuid);
                // Refresh
                Bukkit.getScheduler().runTask(getPlugin(), () -> refresh(info));
                return info;
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, String.format("Failed to fetch player information of %s!", uuid), ex);
            }

            return null;
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> delete(@NotNull UUID uuid) {
        // Delete from cache
        cache.remove(uuid);

        return CompletableFuture.supplyAsync(() -> {
            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_DELETE, table))) {
                // Set
                statement.setString(1, uuid.toString());
                // Execute
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, String.format("Failed to delete player information of %s!", uuid), ex);
                return false;
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> deleteAll() {
        // Clear caches
        cache.clear();
        expirationQueue.clear();

        return CompletableFuture.supplyAsync(() -> {
            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_DELETE_ALL, table))) {
                // Execute
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to delete all player information!", ex);
                return false;
            }
        });
    }

    /**
     * Fetches all queued requests.
     */
    private void fetchAll() {
        // Nothing to fetch
        if (fetch.size() == 0)
            return;

        // Replace
        Set<UUID> queued = fetch;
        fetch = new HashSet<>();

        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            // Build the expression
            StringBuilder expression = new StringBuilder(2 * queued.size() - 1);
            for (int i = 0; i < expression.length() / 2; i++)
                expression.append("?").append(",");
            expression.append("?");

            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(String.format(SQL_FETCH_ALL, table, expression))) {
                // Build the statement
                int i = 1;
                for (UUID uuid : queued)
                    statement.setString(i++, uuid.toString());

                // Execute
                ResultSet resultSet = statement.executeQuery();
                // Fetched
                List<PlayerInfo> fetched = new LinkedList<>();

                // While has next
                while (resultSet.next()) {
                    // UUID
                    UUID uuid = UUID.fromString(resultSet.getString(1));
                    queued.remove(uuid);
                    // Construct
                    fetched.add(PlayerInfo.from(uuid, resultSet.getInt(2), resultSet.getLong(3)));
                }

                // Refresh sync
                Bukkit.getScheduler().runTask(getPlugin(), () -> fetched.forEach(this::refresh));
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to fetch player information!", ex);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // SQL statement
            String sql = String.format(limit <= 0 ? SQL_LEADERBOARD_UNLIMITED : SQL_LEADERBOARD_LIMITED, table);
            // List
            List<PlayerInfo> leaderboard = new ArrayList<>();

            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set limit if applicable
                if (limit > 0)
                    statement.setInt(1, limit);

                // Execute
                ResultSet resultSet = statement.executeQuery();
                // Fetched
                List<PlayerInfo> fetched = new LinkedList<>();

                // While there's anything available
                while (resultSet.next()) {
                    // Info
                    PlayerInfo info = PlayerInfo.from(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2), resultSet.getLong(3));
                    // Refresh
                    fetched.add(info);
                    leaderboard.add(info);
                }

                // Refresh sync
                Bukkit.getScheduler().runTask(getPlugin(), () -> fetched.forEach(this::refresh));
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to fetch leaderboard information!", ex);
                return null;
            }

            // Return
            return leaderboard;
        });
    }

    @Override
    public void close() {
        if (dataSource != null)
            dataSource.close();
    }

    @Override
    public boolean isInstantFetch() {
        return false;
    }

    /**
     * Refreshes the given info by caching it and {@link #passToSampler(PlayerInfo) passing to the sampler}.
     *
     * @param info the info to refresh
     */
    private void refresh(@NotNull PlayerInfo info) {
        // Cache only if enabled
        if (fetchExpiration > 0) {
            cache.put(info.getUniqueId(), info);
            expirationQueue.add(info);
        }
        passToSampler(info);
    }
}