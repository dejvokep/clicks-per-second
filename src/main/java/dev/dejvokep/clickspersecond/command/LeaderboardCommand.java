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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.permission.Permission;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.*;

/**
 * Handler for the <code>/cps leaderboard</code> command.
 */
public class LeaderboardCommand {

    // Utilities
    private final ClicksPerSecond plugin;
    private final Messenger messenger;

    /**
     * Registers the command to the given manager.
     *
     * @param plugin  the plugin
     * @param manager the manager
     */
    public LeaderboardCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
        this.plugin = plugin;
        this.messenger = plugin.getMessenger();

        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("leaderboard").permission("cps.leaderboard")
                .argument(IntegerArgument.optional("page", 1))
                .meta(CommandMeta.DESCRIPTION, "Displays leaderboard information.")
                .flag(manager.flagBuilder("fetch").withAliases("f").withDescription(ArgumentDescription.of("fetch if not available")).withPermission(Permission.of("cps.leaderboard.fetch")))
                .flag(manager.flagBuilder("refresh").withAliases("r").withDescription(ArgumentDescription.of("initiate full refresh")).withPermission(Permission.of("cps.leaderboard.refresh"))).handler(context -> {
                    // Leaderboard
                    List<PlayerInfo> leaderboard = plugin.getDataStorage().getLeaderboard();
                    // Page indexes
                    int perPage = plugin.getConfiguration().getInt(MESSAGE_PREFIX + "leaderboard.entries-per-page"), page = context.get("page"), pages = (int) Math.ceil((double) leaderboard.size() / perPage);
                    Function<String, String> pageReplacer = message -> message.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages));

                    // Invalid page
                    if (page < 1) {
                        messenger.send(context, MESSAGE_PREFIX + "leaderboard.invalid-page", pageReplacer);
                        return;
                    }

                    // Booleans
                    boolean fetch = context.flags().isPresent("fetch"), refresh = context.flags().isPresent("refresh");
                    // Display
                    if (!refresh && displayBoard(context, leaderboard, perPage, page, pages, pageReplacer))
                        return;

                    // Not fetching
                    if (!fetch && !refresh) {
                        messenger.send(context, MESSAGE_PREFIX + "leaderboard.invalid-page", pageReplacer);
                        return;
                    }

                    // Fetch
                    messenger.send(context, MESSAGE_REQUEST_SENT);
                    plugin.getDataStorage().fetchBoard(refresh ? Math.max(plugin.getDataStorage().getLeaderboardLimit(), fetch ? perPage * page : 0) : perPage * page).whenComplete((board, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        // If an error
                        if (board == null) {
                            messenger.send(context, MESSAGE_REQUEST_ERROR);
                            return;
                        }

                        // New indexes
                        int newPages = (int) Math.ceil((double) board.size() / perPage);
                        Function<String, String> newPageReplacer = message -> message.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(newPages));

                        // Display
                        if (!displayBoard(context, board, perPage, page, newPages, newPageReplacer))
                            messenger.send(context, MESSAGE_PREFIX + "leaderboard.invalid-page", newPageReplacer);
                    }));
                }).build());
    }

    /**
     * Displays the leaderboard for the sender of the provided context.
     *
     * @param context      the command context
     * @param board        the leaderboard
     * @param perPage      how many entries to display per page
     * @param page         page to display, greater than <code>0</code>
     * @param pages        total amount of pages available
     * @param pageReplacer page placeholders replacer
     * @return if the leaderboard was displayed, <code>false</code> otherwise, indicating that the page number is out of
     * range
     */
    private boolean displayBoard(CommandContext<CommandSender> context, List<PlayerInfo> board, int perPage, int page, int pages, Function<String, String> pageReplacer) {
        // Invalid page number
        if (page > pages)
            return false;

        // Send header
        messenger.send(context, MESSAGE_PREFIX + "leaderboard.header", pageReplacer);

        // Send entries
        for (int i = perPage * (page - 1); i < perPage * page && i < board.size(); i++) {
            int finalIndex = i;
            messenger.send(context, MESSAGE_PREFIX + "leaderboard.entry", message -> plugin.getPlaceholderReplacer().info(board.get(finalIndex), message.replace("{place}", String.valueOf(finalIndex + 1))));
        }

        // Send footer
        messenger.send(context, MESSAGE_PREFIX + "leaderboard.footer", pageReplacer);
        return true;
    }

}