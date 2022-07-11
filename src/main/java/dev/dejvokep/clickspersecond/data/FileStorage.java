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

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.serialization.standard.StandardSerializer;
import dev.dejvokep.boostedyaml.serialization.standard.TypeAdapter;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Implementation of {@link DataStorage} for files.
 */
public class FileStorage extends DataStorage {

    // File
    private YamlDocument file;

    /**
     * Initializes the data storage.
     *
     * @param plugin the plugin
     */
    public FileStorage(@NotNull ClicksPerSecond plugin) {
        // Call
        super(plugin, "player-data.yml file");

        try {
            // Load
            file = YamlDocument.create(new File(plugin.getDataFolder(), "player-data.yml"), GeneralSettings.builder().setDefaultMap(HashMap::new).setUseDefaults(false).build(), LoaderSettings.DEFAULT, DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
            // Ready
            ready();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player-data.yml file!", ex);
        }
    }

    @Override
    public void sync(@NotNull Set<PlayerInfo> queued) {
        // For each (no need to worry about delayed sync as fetching is immediate)
        queued.forEach(info -> file.set(info.getUniqueId().toString(), info));

        // Save
        try {
            file.save();
        } catch (IOException ex) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to save player information!", ex);
        }
    }

    @Override
    public void queueFetch(@NotNull UUID uuid) {
        passToSampler(file.getAsOptional(uuid.toString(), PlayerInfo.class).orElseGet(() -> PlayerInfo.empty(uuid)));
    }

    @Override
    public void skipFetch(@NotNull UUID uuid) {
        // Unused
    }

    @Override
    @NotNull
    public CompletableFuture<PlayerInfo> fetchSingle(@NotNull UUID uuid, boolean skipCache) {
        return CompletableFuture.completedFuture(file.getAs(uuid.toString(), PlayerInfo.class, PlayerInfo.empty(uuid)));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> delete(@NotNull UUID uuid) {
        // Delete
        file.remove(uuid.toString());

        // Save
        try {
            file.save();
        } catch (IOException ex) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to save player information!", ex);
            return CompletableFuture.completedFuture(false);
        }

        // Success
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> deleteAll() {
        // Clear
        file.clear();

        // Save
        try {
            file.save();
        } catch (IOException ex) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to save player information!", ex);
            return CompletableFuture.completedFuture(false);
        }

        // Success
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<List<PlayerInfo>> fetchLeaderboard(int limit) {
        return CompletableFuture.completedFuture(file.getStoredValue().values().stream().map(block -> (PlayerInfo) block.getStoredValue()).sorted(Comparator.comparingInt(PlayerInfo::getCPS).reversed()).collect(Collectors.toList()));
    }

    @Override
    public void close() {
        // Unused
    }

    @Override
    public boolean isInstantFetch() {
        return true;
    }

    static {
        // Register adapters
        StandardSerializer.getDefault().register(PlayerInfo.class, new TypeAdapter<PlayerInfo>() {
            @NotNull
            @Override
            public Map<Object, Object> serialize(@NotNull PlayerInfo info) {
                Map<Object, Object> map = new HashMap<>();
                map.put("uuid", info.getUniqueId());
                map.put("cps", info.getCPS());
                map.put("time", info.getTime());
                map.put("toggle", info.getToggle());
                return map;
            }

            @NotNull
            @Override
            public PlayerInfo deserialize(@NotNull Map<Object, Object> map) {
                return PlayerInfo.from((UUID) map.get("uuid"), (int) map.get("cps"), (long) map.get("time"));
            }
        });
        StandardSerializer.getDefault().register("cps:player-info", PlayerInfo.class);
    }
}
