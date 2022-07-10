package dev.dejvokep.clickspersecond.data;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
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

public class DatabaseStorage extends DataStorage {

    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s(uuid CHAR(36), cps INT, t BIGINT(20) UNSIGNED, toggle BOOLEAN, PRIMARY KEY(uuid))";
    private static final String SQL_DELETE_ALL = "DELETE FROM %s";
    private static final String SQL_DELETE = "DELETE FROM %s WHERE uuid=?";
    private static final String SQL_SYNC = "INSERT INTO %s(uuid, cps, t, toggle) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE cps = CASE WHEN cps < VALUES(cps) THEN VALUES(cps) ELSE cps END, t = CASE WHEN cps < VALUES(cps) THEN VALUES(t) ELSE t END, toggle = VALUES(toggle)";
    private static final String SQL_FETCH = "SELECT * FROM %s WHERE uuid=?";
    private static final String SQL_FETCH_ALL = "SELECT * FROM %s WHERE uuid IN (%s)";
    private static final String SQL_LEADERBOARD_LIMITED = "SELECT * FROM %s ORDER BY cps LIMIT ?";
    private static final String SQL_LEADERBOARD_LIMITLESS = "SELECT * FROM %s ORDER BY cps";
    private static final long CACHE_CLEAR_DELAY = 20L;

    private final HikariDataSource dataSource;
    private final String table;
    private final long fetchExpiration, fetchRate;
    private final int fetchSize;

    private final Map<UUID, PlayerInfo> cache = new HashMap<>();
    private final Queue<PlayerInfo> expirationQueue = new LinkedList<>();

    private Set<UUID> fetch = new HashSet<>();

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
            while (expirationQueue.size() > 0 && expirationQueue.peek().getFetchTime() + fetchExpiration < System.currentTimeMillis())
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

    public void queueFetch(@NotNull UUID uuid) {
        if (cache.containsKey(uuid)) {
            passToSampler(cache.get(uuid));
            return;
        }

        fetch.add(uuid);

        // If to fetch immediately or if passed the trigger
        if (fetchRate <= 0 || fetch.size() >= fetchSize)
            fetchAll();
    }

    public void skipFetch(@NotNull UUID uuid) {
        fetch.remove(uuid);
    }

    @NotNull
    @Override
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
                PlayerInfo info = resultSet.next() ? PlayerInfo.from(uuid, resultSet.getInt(2), resultSet.getLong(3), resultSet.getBoolean(4)) : PlayerInfo.empty(uuid);
                // Refresh
                refresh(info);
                return info;
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, String.format("Failed to fetch player information of %s!", uuid), ex);
            }

            return null;
        });
    }

    @NotNull
    @Override
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

    @NotNull
    @Override
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

    private void fetchAll() {
        // Replace
        Set<UUID> queued = fetch;
        fetch = new HashSet<>();

        // Iterate
        Iterator<UUID> iterator = queued.iterator();
        while (iterator.hasNext()) {
            // If cached
            PlayerInfo info = cache.get(iterator.next());
            if (info != null) {
                passToSampler(info);
                iterator.remove();
            }
        }

        // Nothing to fetch
        if (queued.size() == 0)
            return;

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
                // While has next
                while (resultSet.next()) {
                    // UUID
                    UUID uuid = UUID.fromString(resultSet.getString(1));
                    queued.remove(uuid);
                    // Construct
                    refresh(PlayerInfo.from(uuid, resultSet.getInt(2), resultSet.getLong(3), resultSet.getBoolean(4)));
                }

                // Remaining were not found
                for (UUID uuid : queued)
                    refresh(PlayerInfo.empty(uuid));
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to fetch player information!", ex);
            }
        });
    }

    @NotNull
    @Override
    public CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // SQL statement
            String sql = String.format(limit <= 0 ? SQL_LEADERBOARD_LIMITLESS : SQL_LEADERBOARD_LIMITED, table);

            // List
            List<PlayerInfo> leaderboard = new ArrayList<>();

            // Connection
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set limit if applicable
                if (limit > 0)
                    statement.setInt(1, limit);

                // Execute
                ResultSet resultSet = statement.executeQuery();
                // While there's anything available
                while (resultSet.next()) {
                    // Info
                    PlayerInfo info = PlayerInfo.from(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2), resultSet.getLong(3), resultSet.getBoolean(4));
                    // Refresh
                    refresh(info);
                    leaderboard.add(info);
                }
                // Close
                resultSet.close();
            } catch (SQLException ex) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to fetch leaderboard information!", ex);
                return null;
            }

            // Sort
            leaderboard.sort(Comparator.comparingInt(PlayerInfo::getCPS).reversed());
            // Return
            return leaderboard;
        });
    }

    @Override
    public boolean isInstantFetch() {
        return false;
    }

    private synchronized void refresh(@NotNull PlayerInfo info) {
        cache.put(info.getUniqueId(), info);
        expirationQueue.add(info);
        passToSampler(info);
    }
}