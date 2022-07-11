package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import dev.dejvokep.clickspersecond.utils.player.UUIDFactory;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.*;

/**
 * Handler for the <code>/cps stats</code> command.
 */
public class StatsCommand {

    /**
     * Registers the command to the given manager.
     *
     * @param plugin  the plugin
     * @param manager the manager
     */
    public StatsCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
        Messenger messenger = plugin.getMessenger();

        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("stats").permission("cps.stats")
                .argument(StringArgument.optional("name|uuid"))
                .meta(CommandMeta.DESCRIPTION, "Displays player statistics either by name or UUID.")
                .handler(context -> {
                    // ID
                    String id = context.getOrDefault("name|uuid", null);
                    UUID uuid;

                    // ID not provided
                    if (id == null) {
                        // Not a player
                        if (!(context.getSender() instanceof Player)) {
                            messenger.send(context, MESSAGE_PLAYERS_ONLY);
                            return;
                        }
                        uuid = ((Player) context.getSender()).getUniqueId();
                    } else {
                        // Parse UUID
                        uuid = UUIDFactory.fromArgument(id);
                        if (uuid == null) {
                            messenger.send(context, MESSAGE_INVALID_NAME, message -> message.replace("{name}", id));
                            return;
                        }
                    }

                    // Sampler
                    Sampler sampler = plugin.getClickHandler().getSampler(uuid);
                    // Online
                    if (sampler != null) {
                        // Loading
                        if (sampler.getInfo().isLoading()) {
                            messenger.send(context, MESSAGE_REQUEST_PENDING);
                            return;
                        }

                        // Empty
                        if (sampler.getInfo().isEmpty()) {
                            messenger.send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                            return;
                        }

                        // Success
                        messenger.send(context, MESSAGE_PREFIX + "stats.online", message -> plugin.getPlaceholderReplacer().all(sampler, message));
                        return;
                    }

                    // If not instant fetch and no permission
                    if (!plugin.getDataStorage().isInstantFetch() && !context.hasPermission("cps.stats.fetch")) {
                        messenger.send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                        return;
                    }

                    // Fetch
                    messenger.send(context, MESSAGE_REQUEST_SENT);
                    plugin.getDataStorage().fetchSingle(uuid).whenComplete((info, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        // If an error
                        if (info == null) {
                            messenger.send(context, MESSAGE_REQUEST_ERROR);
                            return;
                        }

                        // Empty
                        if (info.isEmpty()) {
                            messenger.send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                            return;
                        }

                        // Success
                        messenger.send(context, MESSAGE_PREFIX + "stats.offline", message -> plugin.getPlaceholderReplacer().info(info, message));
                    }));
                }).build());
    }

}