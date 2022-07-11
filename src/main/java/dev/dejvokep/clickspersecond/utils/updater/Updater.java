package dev.dejvokep.clickspersecond.utils.updater;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Updater class which checks for updates of the plugin by using a website API ({@link #URL}). If enabled, checks for
 * updates per the set delay.
 */
public class Updater {

    /**
     * URL used to get the latest version.
     */
    public static final String URL = "https://api.spigotmc.org/legacy/update.php?resource=57214";

    /**
     * Recheck delay in minutes.
     */
    private static final long RECHECK_DELAY = 12 * 60 * 60 * 20L;

    /**
     * Starts the updater (and it's repeating task repeatedly checking for a new update).
     *
     * @param plugin the plugin instance
     */
    public static void watch(@NotNull ClicksPerSecond plugin) {
        // Version
        String version = plugin.getDescription().getVersion();

        // Schedule
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // The latest version
            String latest;
            // Read
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(URL).openStream()))) {
                latest = reader.readLine();
            } catch (IOException ignored) {
                return;
            }

            // New version available
            if (Integer.parseInt(latest.replace(".", "")) > Integer.parseInt(version.replace(".", "")))
                plugin.getLogger().warning("New version " + latest + " is available! You are using version " + version + ".");
        }, 0L, RECHECK_DELAY);
    }

}