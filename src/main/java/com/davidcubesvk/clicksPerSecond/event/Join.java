package com.davidcubesvk.clicksPerSecond.event;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.utils.updater.Updater;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Class with a listener listening to the join event.
 */
public class Join implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        //Check the permission and if the checking for updates is enabled
        if ((!event.getPlayer().hasPermission("cps.admin.updater") && !event.getPlayer().hasPermission("cps.admin.*") &&
                !event.getPlayer().hasPermission("cps.*")) || !ClicksPerSecond.getUpdater().checkForUpdates())
            return;


        //Get message path
        String path = ClicksPerSecond.getUpdater().isNewVersionAvailable() ? "update" : "latest";

        //Send message
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                ClicksPerSecond.getConfiguration().getString("updates.messages." + path)
                        .replace("{version_current}", Updater.CURRENT_VERSION)
                        .replace("{version_latest}", ClicksPerSecond.getUpdater().getLatestVersion())));
    }

}
