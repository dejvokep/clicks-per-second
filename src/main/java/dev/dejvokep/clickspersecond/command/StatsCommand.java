package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class StatsCommand extends PluginCommand {

    /**
     * Pattern for matching and converting to UUID with dashes.
     */
    private static final Pattern UUID_DASH_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    public StatsCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        super(plugin);

        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("stats").permission("cps.stats").argument(StringArgument.single("id")).handler(context -> {
            // The ID
            String id = context.get("id");
            UUID uuid;

            // If a UUID
            if (id.length() == 32 || id.length() == 36) {
                // Parse
                uuid = UUID.fromString(id.length() == 36 ? id : UUID_DASH_PATTERN.matcher(id).replaceAll("$1-$2-$3-$4-$5"));
            } else {
                // Player
                OfflinePlayer player = Bukkit.getOfflinePlayer(id);
                // Cannot find
                if (!player.hasPlayedBefore()) {
                    send(context, MESSAGE_PREFIX + "stats.invalid-name");
                    return;
                }
                // Set
                uuid = player.getUniqueId();
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
                    send(context, MESSAGE_PREFIX + "stats.not-found");
                    return;
                }

                // Send
                send(context, MESSAGE_PREFIX + "stats.message", message -> plugin.getPlaceholderReplacer().replace(sampler, message));
                return;
            }

            // If not instant fetch and no permission
            if (!plugin.getDataStorage().isInstantFetch() && !context.hasPermission("cps.stats.fetch")) {
                send(context, MESSAGE_NO_PERMISSION);
                return;
            }

            // Message
            send(context, MESSAGE_REQUEST_SENT);

            // Fetch
            plugin.getDataStorage().fetchSingle(uuid).whenComplete((info, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
                // If an error
                if (exception != null) {
                    send(context, MESSAGE_REQUEST_ERROR);
                    plugin.getLogger().log(Level.SEVERE, "An error occurred whilst fetching player data!", exception);
                    return;
                }

                // Empty
                if (info.isEmpty()) {
                    send(context, MESSAGE_PREFIX + "stats.not-found");
                    return;
                }

                // Send
                send(context, MESSAGE_PREFIX + "stats.message", message -> plugin.getPlaceholderReplacer().replace(info, message));
            }));
        }).build());
    }

}