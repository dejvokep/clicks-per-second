package dev.dejvokep.clickspersecond.listener;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
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
        // Remove
        plugin.getClickHandler().remove(event.getPlayer());
        plugin.getDataStorage().skipFetch(event.getPlayer().getUniqueId());
        plugin.getDisplays().forEach(display -> display.remove(event.getPlayer()));
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR)
            plugin.getClickHandler().processClick(event.getPlayer());
    }

}