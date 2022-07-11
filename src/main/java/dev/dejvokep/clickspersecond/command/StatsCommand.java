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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.permission.CommandPermission;
import cloud.commandframework.permission.Permission;
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
                .flag(manager.flagBuilder("refresh").withAliases("r").withDescription(ArgumentDescription.of("initiate refresh")).withPermission(Permission.of("cps.stats.refresh")))
                .meta(CommandMeta.DESCRIPTION, "Displays player statistics either by name or UUID.")
                .handler(context -> {
                    // ID
                    String id = context.getOrDefault("name|uuid", null);
                    UUID uuid;
                    // Refresh
                    boolean refresh = context.flags().isPresent("refresh");

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
                    if (sampler != null && !refresh) {
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
                    if (!refresh && !plugin.getDataStorage().isInstantFetch() && !context.hasPermission("cps.stats.fetch")) {
                        messenger.send(context, MESSAGE_PREFIX + "stats.not-found", message -> plugin.getPlaceholderReplacer().player(uuid, message));
                        return;
                    }

                    // Fetch
                    messenger.send(context, MESSAGE_REQUEST_SENT);
                    plugin.getDataStorage().fetchSingle(uuid, refresh).whenComplete((info, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
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