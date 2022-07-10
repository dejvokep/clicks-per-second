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
import dev.dejvokep.clickspersecond.listener.ConnectionListener;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import dev.dejvokep.clickspersecond.utils.placeholders.PlaceholderReplacer;
import dev.dejvokep.clickspersecond.utils.placeholders.StatsExpansion;
import dev.dejvokep.clickspersecond.utils.watcher.Watchers;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Main plugin class.
 */
public class ClicksPerSecond extends JavaPlugin implements Listener {

    private ClickHandler clickHandler;
    private final Set<Display> displays = new HashSet<>();

    private YamlDocument config;
    private DataStorage dataStorage;
    private PlaceholderReplacer placeholderReplacer;
    private Watchers watchers;
    private Messenger messenger;

    @Override
    public void onEnable() {
        // Thank you message
        getLogger().info("Thank you for downloading ClicksPerSecond!");

        try {
            // Create the config file
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config2.yml")), GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build());
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize the config file!", ex);
            return;
        }

        // Initialize
        placeholderReplacer = new PlaceholderReplacer(this);
        watchers = new Watchers();
        messenger = new Messenger(this);

        // Handlers
        int samplingRate = Math.max(config.getInt("sampling-rate"), 0);
        clickHandler = samplingRate == 0 ? new ImmediateHandler(this) : new RatedHandler(this, samplingRate);

        // Add displays
        displays.add(new ActionBarDisplay(this));
        displays.add(new BossBarDisplay(this));
        displays.add(new TitleDisplay(this));

        // Register placeholders
        if (PlaceholderReplacer.PLACEHOLDER_API_AVAILABLE) {
            new StatsExpansion(this).register();
            getLogger().info("Registered plugin's placeholders to PlaceholderAPI.");
        }

        try {
            CommandManager<CommandSender> commandManager = new BukkitCommandManager<>(this, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity());
            new StatsCommand(this, commandManager);
            new LeaderboardCommand(this, commandManager);
            new DeleteCommand(this, commandManager);
            new WatchCommand(this, commandManager);
            new ReloadCommand(this, commandManager);
            new ConfirmCommand(this, commandManager);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Run async
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Storage
            dataStorage = config.getString("storage").equalsIgnoreCase("FILE") ? new FileStorage(this) : new DatabaseStorage(this);
            // Register listeners
            Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this));
        });
    }

    @Override
    public void onDisable() {
    }

    @NotNull
    public YamlDocument getConfiguration() {
        return config;
    }

    @NotNull
    public DataStorage getDataStorage() {
        return dataStorage;
    }

    @NotNull
    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    @NotNull
    public Set<Display> getDisplays() {
        return displays;
    }

    @NotNull
    public PlaceholderReplacer getPlaceholderReplacer() {
        return placeholderReplacer;
    }

    @NotNull
    public Watchers getWatchers() {
        return watchers;
    }

    @NotNull
    public Messenger getMessenger() {
        return messenger;
    }
}