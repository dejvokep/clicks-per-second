package dev.dejvokep.clickspersecond;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import dev.dejvokep.clickspersecond.command.StatsCommand;
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
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Handlers
        int samplingRate = Math.max(config.getInt("sampling-rate"), 0);
        clickHandler = samplingRate == 0 ? new ImmediateHandler(this) : new RatedHandler(this, samplingRate);

        // Add displays
        displays.add(new ActionBarDisplay(this));
        displays.add(new BossBarDisplay(this));
        displays.add(new TitleDisplay(this));

        // Register placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StatsExpansion(this).register();
            getLogger().info("Registered plugin's placeholders to PlaceholderAPI.");
        }

        try {
            new StatsCommand(this, new BukkitCommandManager<>(this, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity()));
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

    public YamlDocument getConfiguration() {
        return config;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    public Set<Display> getDisplays() {
        return displays;
    }

    public PlaceholderReplacer getPlaceholderReplacer() {
        return placeholderReplacer;
    }

}