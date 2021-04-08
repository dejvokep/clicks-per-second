package com.davidcubesvk.clicksPerSecond;

import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.command.BaseCommand;
import com.davidcubesvk.clicksPerSecond.command.cpsTest.TestCommand;
import com.davidcubesvk.clicksPerSecond.command.cpsTest.TestCommandExecutor;
import com.davidcubesvk.clicksPerSecond.event.Interact;
import com.davidcubesvk.clicksPerSecond.event.Join;
import com.davidcubesvk.clicksPerSecond.event.Quit;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import com.davidcubesvk.clicksPerSecond.utils.command.DynamicCommandRegisterer;
import com.davidcubesvk.clicksPerSecond.utils.data.DataStorageOperator;
import com.davidcubesvk.clicksPerSecond.utils.data.file.FileWriter;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import com.davidcubesvk.clicksPerSecond.utils.packets.PacketHandler;
import com.davidcubesvk.clicksPerSecond.utils.placeholders.Placeholders;
import com.davidcubesvk.clicksPerSecond.utils.updater.Updater;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Main plugin class.
 */
public class ClicksPerSecond extends JavaPlugin implements Listener {

    //Plugin instance
    private static Plugin plugin = null;

    //Config and file instance
    private static final FileConfiguration config = new YamlConfiguration();
    private static File configFile;
    //Scoreboard and file instance
    private static final FileConfiguration scoreboard = new YamlConfiguration();
    private static File scoreboardFile;

    //Server version
    public static final String SERVER_VERSION = Bukkit.getBukkitVersion();

    //Storage type
    private static StorageType storageType;
    //Updater instance
    private static Updater updater;

    //Database instance
    private static Database database;
    //FileWriter
    private static FileWriter fileWriter;

    //TestManager instance
    private static TestManager testManager;
    //API instance
    private static ClicksPerSecondAPI clicksPerSecondAPI;
    //Reformatter instance
    private static Reformatter reformatter;
    //TestCommand instance
    private static TestCommandExecutor testCommand;

    /**
     * Enum representing listener types.
     * - EVENTS: listen using Bukkit events
     * - PACKETS: listen using packets registered by ProtocolLib
     */
    private enum ListenerType {
        EVENTS, PACKETS
    }

    @Override
    public void onEnable() {
        //Set plugin instance
        plugin = this;

        //Thank you
        getLogger().log(Level.INFO, "Thank you for choosing ClicksPerSecond plugin!");

        //Register command
        Bukkit.getPluginCommand("cps").setExecutor(new BaseCommand());

        //Register events
        Bukkit.getPluginManager().registerEvents(new Join(), this);
        Bukkit.getPluginManager().registerEvents(new Quit(), this);

        //Create folder
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        //Create the abstract instance of the file
        configFile = new File(getDataFolder(), "config.yml");
        //If the file doesn't exist
        if (!configFile.exists())
            //Create the file and configuration in it
            saveResource("config.yml", false);

        //Create the abstract instance of the file
        scoreboardFile = new File(getDataFolder(), "scoreboard.yml");
        //If the file doesn't exist
        if (!scoreboardFile.exists())
            //Create the file and configuration in it
            saveResource("scoreboard.yml", false);
        //Reload files
        reload();

        try {
            //Try to parse
            storageType = StorageType.valueOf(config.getString("dataStorage").toUpperCase());

            //Log
            getLogger().log(Level.INFO, "Data storage is set to " + storageType.name() + ".");
        } catch (IllegalArgumentException ex) {
            //Invalid type
            getLogger().log(Level.WARNING, "Invalid dataStorage option! Using default value FILE.");
            return;
        }

        //How to register clicks
        ListenerType listenerType;
        try {
            //Try to parse
            listenerType = ListenerType.valueOf(config.getString("listenerType").toUpperCase());

            //Log
            getLogger().log(Level.INFO, "Listener type is set to " + listenerType.name() + ".");
        } catch (IllegalArgumentException ex) {
            //Invalid type
            getLogger().log(Level.WARNING, "Invalid listenerType option! Using default value EVENTS.");
            return;
        }

        //Connect database if it is the target storage type
        if (storageType == StorageType.DATABASE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    //Initialize database data
                    database = new Database();
                    //Connect and create tables
                    database.connect();
                }
            }.runTaskAsynchronously(this);
        }

        //Handle packets if ListenerType == PACKETS
        if (listenerType == ListenerType.PACKETS)
            //Register packet listeners
            new PacketHandler();
        else
            //Register event
            Bukkit.getPluginManager().registerEvents(new Interact(), this);

        //Register placeholder extension if PlaceholderAPI enabled
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            //Register extension
            new Placeholders().register();
            //Info
            getLogger().log(Level.INFO, "Placeholder extension registered successfully.");
        } else {
            //Failed to register
            getLogger().log(Level.WARNING, "Failed to register placeholder extension! Please make sure you have PlaceholderAPI installed.");
        }

        //Initialize TestCommand
        testCommand = new TestCommandExecutor();
        //Create dynamic registerer
        DynamicCommandRegisterer dynamicRegisterer = new DynamicCommandRegisterer();
        //Register commands dynamically
        for (String command : config.getStringList("test.command.main")) {
            //If not the /cps command
            if (command.equals("cps"))
                continue;

            //Register
            dynamicRegisterer.register(new TestCommand(command));
        }

        //Initialize TestManager
        testManager = new TestManager();
        //Initialize Reformatter
        reformatter = new Reformatter();
        //Initialize FileWriter
        fileWriter = new FileWriter();
        //Initialize API
        clicksPerSecondAPI = new ClicksPerSecondAPI();

        //Initialize updater asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                updater = new Updater();
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onDisable() {
        //If saving into database, disconnect
        if (storageType == StorageType.DATABASE && database != null)
            //Disconnect
            database.disconnect();

        //Set plugin to null
        plugin = null;
    }

    /**
     * Reloads config.yml and scoreboard.yml file.
     */
    public static void reload() {
        try {
            config.load(configFile);
            scoreboard.load(scoreboardFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves scoreboard.yml file.
     */
    public static void saveScoreboard() {
        try {
            scoreboard.save(scoreboardFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns a data storage operator ({@link FileWriter} or {@link Database}) by the specified storage type.
     * If the parameter is <code>null</code>, returns {@link FileWriter}.
     *
     * @param storageType the storage type which is operated by the operator
     * @return the data storage operator
     */
    public static DataStorageOperator getStorageOperatorByType(StorageType storageType) {
        //Return database
        if (storageType == StorageType.DATABASE)
            return database;

        //Return FileWriter by default
        return fileWriter;
    }

    /**
     * Returns the config.yml file.
     *
     * @return the config.yml file
     */
    public static FileConfiguration getConfiguration() {
        return config;
    }

    /**
     * Returns the scoreboard.yml file.
     *
     * @return the scoreboard.yml file
     */
    public static FileConfiguration getScoreboard() {
        return scoreboard;
    }

    /**
     * Returns the instance of this plugin.
     *
     * @return the instance of this plugin
     */
    public static Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the updater.
     *
     * @return the updater
     */
    public static Updater getUpdater() {
        return updater;
    }

    /**
     * Returns the CPS-test manager.
     *
     * @return the CPS-test manager
     */
    public static TestManager getTestManager() {
        return testManager;
    }

    /**
     * Returns the database operator.
     *
     * @return the database operator
     * @see #getStorageOperatorByType(StorageType)
     */
    public static Database getDatabase() {
        return database;
    }

    /**
     * Returns the file operator.
     *
     * @return the file operator
     * @see #getStorageOperatorByType(StorageType)
     */
    public static FileWriter getFileWriter() {
        return fileWriter;
    }

    /**
     * Returns the reformatter.
     *
     * @return the reformatter
     */
    public static Reformatter getReformatter() {
        return reformatter;
    }

    /**
     * Returns the currently used storage type.
     *
     * @return the currently used storage type
     */
    public static StorageType getStorageType() {
        return storageType;
    }

    /**
     * Returns the test command executor.
     *
     * @return the test command executor
     */
    public static TestCommandExecutor getTestCommandExecutor() {
        return testCommand;
    }

    /**
     * Returns the API.
     *
     * @return the API
     */
    public static ClicksPerSecondAPI getClicksPerSecondAPI() {
        return clicksPerSecondAPI;
    }
}