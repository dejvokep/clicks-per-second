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
package dev.dejvokep.clickspersecond;

import cloud.commandframework.CommandManager;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import dev.dejvokep.clickspersecond.command.*;
import dev.dejvokep.clickspersecond.data.DataStorage;
import dev.dejvokep.clickspersecond.data.DatabaseStorage;
import dev.dejvokep.clickspersecond.data.FileStorage;
import dev.dejvokep.clickspersecond.display.implementation.ActionBarDisplay;
import dev.dejvokep.clickspersecond.display.implementation.BossBarDisplay;
import dev.dejvokep.clickspersecond.display.Display;
import dev.dejvokep.clickspersecond.display.implementation.TitleDisplay;
import dev.dejvokep.clickspersecond.handler.ClickHandler;
import dev.dejvokep.clickspersecond.handler.ImmediateHandler;
import dev.dejvokep.clickspersecond.handler.RatedHandler;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.listener.EventListeners;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import dev.dejvokep.clickspersecond.utils.placeholders.PlaceholderReplacer;
import dev.dejvokep.clickspersecond.utils.placeholders.StatsExpansion;
import dev.dejvokep.clickspersecond.utils.updater.Updater;
import dev.dejvokep.clickspersecond.utils.watcher.WatchManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * The main plugin class.
 */
public class ClicksPerSecond extends JavaPlugin implements Listener {

    // Displays
    private Set<Display> displays = new HashSet<>();

    // Data
    private YamlDocument config;
    private DataStorage dataStorage;

    // Internals
    private ClickHandler<? extends Sampler> clickHandler;
    private PlaceholderReplacer placeholderReplacer;
    private WatchManager watchManager;
    private EventListeners listeners;
    private Messenger messenger;

    @Override
    public void onEnable() {
        // Thank you message
        getLogger().info("Thank you for downloading ClicksPerSecond!");

        try {
            // Create the config file
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config.yml")), GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build());
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize the config file!", ex);
            return;
        }

        // Initialize
        placeholderReplacer = new PlaceholderReplacer(this);
        watchManager = new WatchManager();
        messenger = new Messenger(this);
        Updater.watch(this);

        // Handlers
        int samplingRate = Math.max(config.getInt("sampling-rate"), 0);
        clickHandler = samplingRate == 0 ? new ImmediateHandler(this) : new RatedHandler(this, samplingRate);

        // Add displays
        displays.add(new ActionBarDisplay(this));
        displays.add(new BossBarDisplay(this));
        displays.add(new TitleDisplay(this));
        displays = Collections.unmodifiableSet(displays);

        // Register placeholders
        if (PlaceholderReplacer.PLACEHOLDER_API_AVAILABLE) {
            new StatsExpansion(this).register();
            getLogger().info("Registered plugin's placeholders to PlaceholderAPI.");
        }

        // Commands
        try {
            CommandManager<CommandSender> commandManager = new BukkitCommandManager<>(this, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity());
            new StatsCommand(this, commandManager);
            new LeaderboardCommand(this, commandManager);
            new DeleteCommand(this, commandManager);
            new WatchCommand(this, commandManager);
            new ReloadCommand(this, commandManager);
            new ConfirmCommand(this, commandManager);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "An unexpected error occurred whilst registering commands!", ex);
        }

        // Run async
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Storage
            dataStorage = config.getString("storage").equalsIgnoreCase("FILE") ? new FileStorage(this) : new DatabaseStorage(this);

            // Back to sync
            Bukkit.getScheduler().runTask(this, () -> {
                // Register listeners
                Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().registerEvents(listeners = new EventListeners(this), this));

                // Add all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    clickHandler.add(player);
                    displays.forEach(display -> display.add(player));
                }
            });
        });

        // If enabled
        if (config.getBoolean("metrics")) {
            getLogger().info("Initializing metrics.");
            new Metrics(this, 15733);
        }
    }

    @Override
    public void onDisable() {
        dataStorage.close();
    }

    /**
     * Returns the plugin configuration.
     *
     * @return the plugin configuration
     */
    @NotNull
    public YamlDocument getConfiguration() {
        return config;
    }


    /**
     * Returns the data storage.
     *
     * @return the data storage
     */
    @NotNull
    public DataStorage getDataStorage() {
        return dataStorage;
    }

    /**
     * Returns the click handler.
     *
     * @return the click handler
     */
    @NotNull
    public ClickHandler<? extends Sampler> getClickHandler() {
        return clickHandler;
    }

    /**
     * Returns an unmodifiable set of all available displays.
     *
     * @return the unmodifiable set of all available displays
     */
    @NotNull
    public Set<Display> getDisplays() {
        return displays;
    }

    /**
     * Returns the placeholder replacer.
     *
     * @return the placeholder replacer
     */
    @NotNull
    public PlaceholderReplacer getPlaceholderReplacer() {
        return placeholderReplacer;
    }

    /**
     * Returns the watch manager.
     *
     * @return the watch manager
     */
    @NotNull
    public WatchManager getWatchManager() {
        return watchManager;
    }

    /**
     * Returns the event listeners.
     *
     * @return the event listeners
     */
    public EventListeners getListeners() {
        return listeners;
    }

    /**
     * Returns the messenger.
     *
     * @return the messenger
     */
    @NotNull
    public Messenger getMessenger() {
        return messenger;
    }
}