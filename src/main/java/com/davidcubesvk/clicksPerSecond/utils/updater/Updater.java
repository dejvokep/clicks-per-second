package com.davidcubesvk.clicksPerSecond.utils.updater;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

/**
 * Updater class.
 */
public class Updater {

    //URL to get latest version
    private static final String url = "https://api.spigotmc.org/legacy/update.php?resource=57214";
    //If is new version available and if to check that
    private boolean isNewVersion, check;
    //Refresh delay in minutes:
    private int refresh;

    //Current version and the latest version of the plugin
    public static String CURRENT_VERSION = ClicksPerSecond.getPlugin().getDescription().getVersion();
    private String latestVersion;

    /**
     * Starts the updater.
     */
    public Updater() {
        //Reload
        reload();

        //Re-check for updates
        if (refresh != -1) {
            //Check if not invalid
            if (refresh < 1) {
                //Warn
                ClicksPerSecond.getPlugin().getLogger().log(Level.WARNING, "Updater refresh rate is smaller than 1 hour! Using value 1 hour.");
                refresh = 1;
            }

            //Schedule
            Bukkit.getScheduler().runTaskTimer(ClicksPerSecond.getPlugin(), this::refresh, 72000L * refresh, 72000L * refresh);
        }
    }

    /**
     * Reloads internal data.
     */
    public void reload() {
        //Get if checking is enabled
        this.check = ClicksPerSecond.getConfiguration().getBoolean("updates.check");
        //Get the refresh delay
        this.refresh = ClicksPerSecond.getConfiguration().getInt("updates.checkDelay");
        //Refresh
        refresh();
    }

    /**
     * Refreshes the latest version of the plugin.
     */
    private void refresh() {
        Bukkit.getScheduler().runTaskAsynchronously(ClicksPerSecond.getPlugin(), () -> {
            if (!check) return;

            //Print to console
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Checking for updates...");

            //Get current version of plugin
            int currentVersionNumbers = Integer.parseInt(CURRENT_VERSION.replace(".", ""));

            //Get latest version of plugin
            try {
                this.latestVersion = new BufferedReader(new InputStreamReader(new URL(url).openStream())).readLine();
            } catch (IOException ex) {
                //Error
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "Failed to check for updates.");
                //Return
                return;
            }

            //Get latest version in numbers
            int latestVersionNumbers = Integer.parseInt(this.latestVersion.replace(".", ""));
            this.isNewVersion = latestVersionNumbers > currentVersionNumbers;

            //Log and print to console
            if (isNewVersion) {
                //New version available
                ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "New version " + latestVersion + " is available! You are using version " + CURRENT_VERSION + ".");
            } else {
                //Using the latest version
                ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "You are using the latest version " + CURRENT_VERSION + "! No updates available.");
            }
        });
    }

    /**
     * Returns if new version is available.
     *
     * @return if new version is available
     */
    public boolean isNewVersionAvailable() {
        return isNewVersion;
    }

    /**
     * Returns the latest version string (X.X).
     *
     * @return the latest version string
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Returns if checking for updates is enabled.
     *
     * @return if checking for updates is enabled
     */
    public boolean checkForUpdates() {
        return check;
    }

    /**
     * Returns the class instance.
     *
     * @return the class instance
     */
    public static Updater getInstance() {
        return ClicksPerSecond.getUpdater();
    }
}
