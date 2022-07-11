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
package dev.dejvokep.clickspersecond.listener;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Class containing event listeners necessary for the plugin.
 */
public class EventListeners implements Listener {

    // Plugin
    private final ClicksPerSecond plugin;
    // Entity clicks only
    private boolean entityClicksOnly;

    /**
     * Initializes (but does not register) this event listener. Automatically calls {@link #reload()}.
     *
     * @param plugin the plugin
     */
    public EventListeners(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
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
        plugin.getWatchManager().consumeWatchers(player, watcher -> {
            // Stop
            plugin.getWatchManager().stop(watcher);
            plugin.getMessenger().send(watcher, Messenger.MESSAGE_PREFIX + "watch.disconnected", message -> plugin.getPlaceholderReplacer().player(player.getUniqueId(), message));
        });

        // If watching
        plugin.getWatchManager().stop(player);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK && !entityClicksOnly)
            plugin.getClickHandler().processClick(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        plugin.getClickHandler().processClick(event.getDamager().getUniqueId());
    }

    /**
     * Reloads internal configuration.
     */
    public void reload() {
        entityClicksOnly = plugin.getConfiguration().getBoolean("entity-clicks-only");
    }

}