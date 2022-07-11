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
package dev.dejvokep.clickspersecond.display.implementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import dev.dejvokep.clickspersecond.utils.watcher.VariableMessages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Action bar display.
 */
public class ActionBarDisplay implements Display {

    /**
     * Indicates if to use packets to send action bar messages.
     */
    private static final boolean USE_PACKETS = Bukkit.getBukkitVersion().contains("1.8") || Bukkit.getBukkitVersion().contains("1.9");

    /**
     * The max refresh rate (delay between each refresh).
     */
    private static final long MAX_REFRESH_RATE = 35L;

    // Players
    private final Set<Player> players = new HashSet<>();

    // Internals
    private final ClicksPerSecond plugin;
    private BukkitTask task;
    private VariableMessages<String> message;

    /**
     * Initializes the display. Automatically calls {@link #reload()}.
     *
     * @param plugin the plugin
     */
    public ActionBarDisplay(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void add(@NotNull Player player) {
        // If the task is not running
        if (task == null)
            return;
        // Add
        players.add(player);
    }

    @Override
    public void remove(@NotNull Player player) {
        // If the task is not running
        if (task == null)
            return;

        // Remove
        if (players.remove(player))
            send(player, "");
    }

    @Override
    public void reload() {
        // Config
        Section config = plugin.getConfiguration().getSection("display.action-bar");

        // Cancel
        if (task != null) {
            task.cancel();
            task = null;
        }

        // If disabled
        if (!config.getBoolean("enabled") || (USE_PACKETS && !Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")))
            return;

        // Set
        message = VariableMessages.of(plugin, config.getSection("message"));
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> players.forEach(player -> send(player, message.get(player, (message, target) -> plugin.getPlaceholderReplacer().api(target, message)))), 0L, Math.min(MAX_REFRESH_RATE, Math.max(config.getInt("refresh"), plugin.getClickHandler().getMinDisplayRate())));
    }

    /**
     * Sends the message to the given player.
     *
     * @param player  the player to send to
     * @param message the message to send
     */
    private void send(@NotNull Player player, @NotNull String message) {
        // If not to use packets
        if (!USE_PACKETS) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            return;
        }

        try {
            // Create
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
            // Set chat type
            packet.getBytes().writeSafely(0, (byte) 2);
            packet.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
            // Write the message
            packet.getChatComponents().write(0, WrappedChatComponent.fromText(message));
            // Send
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send an action bar message!", ex);
        }
    }
}