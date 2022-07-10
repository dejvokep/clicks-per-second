package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_PLAYERS_ONLY;
import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_PREFIX;

public class WatchCommand {

    public WatchCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
        Messenger messenger = plugin.getMessenger();

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("watch").permission("cps.watch")
                .argument(StringArgument.optional("name"))
                .meta(CommandMeta.DESCRIPTION, "Watches CPS of another player.")
                .handler(context -> {
                    // Not a player
                    if (!(context.getSender() instanceof Player)) {
                        messenger.send(context, MESSAGE_PLAYERS_ONLY);
                        return;
                    }

                    Player sender = (Player) context.getSender();

                    // No name
                    if (!context.contains("name")) {
                        // Stop
                        Player watched = plugin.getWatchers().stop(sender);
                        if (watched == null)
                            messenger.send(context, MESSAGE_PREFIX + "watch.error.not-watching");
                        else
                            messenger.send(context, MESSAGE_PREFIX + "watch.stop", message -> plugin.getPlaceholderReplacer().player(watched, message));
                        return;
                    }

                    // Player
                    Player watched = Bukkit.getPlayerExact(context.get("name"));
                    if (watched == null) {
                        messenger.send(context, MESSAGE_PREFIX + "watch.error.player-offline", message -> message.replace("{name}", context.get("name")));
                        return;
                    }

                    // Trying to watch themselves
                    if (watched == sender) {
                        messenger.send(context, MESSAGE_PREFIX + "watch.error.yourself");
                        return;
                    }

                    // Start
                    plugin.getWatchers().start(sender, watched);
                    // Success
                    messenger.send(context, MESSAGE_PREFIX + "watch.start", message -> plugin.getPlaceholderReplacer().player(watched, message));
                }).build());
    }

}