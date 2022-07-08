package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.UUIDFactory;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class StatsCommand extends PluginCommand {

    public StatsCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        super(plugin);

        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("stats").permission("cps.stats")
                .argument(StringArgument.single("id"))
                .meta(CommandMeta.DESCRIPTION, "Displays player statistics either by name or UUID.")
                .handler(context -> {
                    // Parse UUID
                    String id = context.get("id");
                    UUID uuid = UUIDFactory.fromArgument(id);
                    if (uuid == null) {
                        send(context, MESSAGE_INVALID_NAME, message -> message.replace("{name}", id));
                        return;
                    }

                    // Sampler
                    Sampler sampler = plugin.getClickHandler().getSampler(uuid);
                    // Online
                    if (sampler != null) {
                        // Loading
                        if (sampler.getInfo().isLoading()) {
                            send(context, MESSAGE_REQUEST_PENDING);
                            return;
                        }

                        // Empty
                        if (sampler.getInfo().isEmpty()) {
                            send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                            return;
                        }

                        // Success
                        send(context, MESSAGE_PREFIX + "stats.online", message -> plugin.getPlaceholderReplacer().all(sampler, message));
                        return;
                    }

                    // If not instant fetch and no permission
                    if (!plugin.getDataStorage().isInstantFetch() && !context.hasPermission("cps.stats.fetch")) {
                        send(context, MESSAGE_NO_PERMISSION);
                        return;
                    }

                    // Fetch
                    send(context, MESSAGE_REQUEST_SENT);
                    plugin.getDataStorage().fetchSingle(uuid).whenComplete((info, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        // If an error
                        if (info == null) {
                            send(context, MESSAGE_REQUEST_ERROR);
                            return;
                        }

                        // Empty
                        if (info.isEmpty()) {
                            send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                            return;
                        }

                        // Success
                        send(context, MESSAGE_PREFIX + "stats.offline", message -> plugin.getPlaceholderReplacer().info(info, message));
                    }));
                }).build());
    }

}