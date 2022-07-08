package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class WatchCommand extends PluginCommand {

    public WatchCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        super(plugin);

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("watch").permission("cps.watch")
                .argument(StringArgument.optional("name"))
                .meta(CommandMeta.DESCRIPTION, "Watches CPS of another player.")
                .handler(context -> {
                    // Not a player
                    if (!(context.getSender() instanceof Player)) {
                        send(context, MESSAGE_PLAYERS_ONLY);
                        return;
                    }

                    Player sender = (Player) context.getSender();

                    // No name
                    if (!context.contains("name")) {
                        // Stop
                        Player watched = getPlugin().getWatchers().stop(sender);
                        if (watched == null)
                            send(context, MESSAGE_PREFIX + "watch.error.not-watching");
                        else
                            send(context, MESSAGE_PREFIX + "watch.stop", message -> message.replace("{name}", watched.getName()));
                        return;
                    }

                    // Name
                    String name = context.get("name");
                    Function<String, String> nameReplacer = message -> message.replace("{name}", name);

                    // Player
                    Player watched = Bukkit.getPlayerExact(context.get("name"));
                    if (watched == null) {
                        send(context, MESSAGE_PREFIX + "watch.error.player-offline", nameReplacer);
                        return;
                    }

                    // Trying to watch themselves
                    if (watched == sender) {
                        send(context, MESSAGE_PREFIX + "watch.error.yourself", nameReplacer);
                        return;
                    }

                    // Start
                    getPlugin().getWatchers().start(sender, watched);
                    // Success
                    send(context, MESSAGE_PREFIX + "watch.start", nameReplacer);
                }).build());
    }

}