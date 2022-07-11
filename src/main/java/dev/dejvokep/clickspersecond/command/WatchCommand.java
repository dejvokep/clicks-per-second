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

/**
 * Handler for the <code>/cps watch</code> command.
 */
public class WatchCommand {

    /**
     * Registers the command to the given manager.
     *
     * @param plugin  the plugin
     * @param manager the manager
     */
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
                    Player watched = plugin.getWatchManager().getWatched(sender);

                    // No name
                    if (!context.contains("name") || (watched != null && watched.getName().equals(context.get("name")))) {
                        // Stop
                        final Player finalWatched = watched;
                        plugin.getWatchManager().stop(sender);
                        if (watched == null)
                            messenger.send(context, MESSAGE_PREFIX + "watch.error.not-watching");
                        else
                            messenger.send(context, MESSAGE_PREFIX + "watch.stop", message -> plugin.getPlaceholderReplacer().player(finalWatched, message));
                        return;
                    }

                    // Player
                    watched = Bukkit.getPlayerExact(context.get("name"));
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
                    final Player finalWatched = watched;
                    plugin.getWatchManager().start(sender, watched);
                    // Success
                    messenger.send(context, MESSAGE_PREFIX + "watch.start", message -> plugin.getPlaceholderReplacer().player(finalWatched, message));
                }).build());
    }

}