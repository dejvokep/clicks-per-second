package dev.dejvokep.clickspersecond.display.implementation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import dev.dejvokep.clickspersecond.utils.watcher.VariableMessage;
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

public class ActionBarDisplay implements Display {

    private static final boolean USE_PACKETS = Bukkit.getBukkitVersion().contains("1.8") || Bukkit.getBukkitVersion().contains("1.9");

    private final Set<Player> players = new HashSet<>();
    private final ClicksPerSecond plugin;

    private BukkitTask task;
    private VariableMessage<String> message;

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
    public void removeAll() {
        players.forEach(this::remove);
    }

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
        message = VariableMessage.of(plugin, config.getSection("message"));
        // Schedule
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> players.forEach(player -> send(player, message.get(player, (message, target) -> plugin.getPlaceholderReplacer().api(target, message)))), 0L, Math.max(config.getInt("refresh"), plugin.getClickHandler().getDisplayRate()));
    }

    private void send(@NotNull Player player, @NotNull String message) {
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
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send action bar!", ex);
        }
    }
}