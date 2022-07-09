package dev.dejvokep.clickspersecond.listener;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private final ClicksPerSecond plugin;

    public ConnectionListener(ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Add
        plugin.getClickHandler().add(event.getPlayer());
        plugin.getDisplays().forEach(display -> display.add(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Player
        Player player = event.getPlayer();
        // Remove
        plugin.getClickHandler().remove(player);
        plugin.getDataStorage().skipFetch(player.getUniqueId());
        plugin.getDisplays().forEach(display -> display.remove(player));

        // Watcher
        Player watcher = plugin.getWatchers().getWatcher(player);
        // If watched
        if (watcher != null) {
            // Stop
            plugin.getWatchers().stop(watcher);
            plugin.getMessenger().send(watcher, Messenger.MESSAGE_PREFIX + "watch.disconnected", message -> plugin.getPlaceholderReplacer().player(player.getUniqueId(), message));
        }

        // If watching
        plugin.getWatchers().stop(player);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR)
            plugin.getClickHandler().processClick(event.getPlayer());
    }

}