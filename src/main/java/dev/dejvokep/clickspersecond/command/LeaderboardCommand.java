package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.messaging.Messenger;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.*;

public class LeaderboardCommand {

    private final ClicksPerSecond plugin;
    private final Messenger messenger;

    public LeaderboardCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
        this.plugin = plugin;
        this.messenger = plugin.getMessenger();

        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("leaderboard").permission("cps.leaderboard")
                .argument(IntegerArgument.optional("page", 1))
                .meta(CommandMeta.DESCRIPTION, "Displays leaderboard information.")
                .flag(manager.flagBuilder("fetch").withAliases("f").withDescription(ArgumentDescription.of("fetch if not available"))).handler(context -> {
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

                    // Display
                    if (displayBoard(context, leaderboard, perPage, page, pages, pageReplacer))
                        return;

                    // Fetch not present
                    if (!context.flags().isPresent("fetch"))
                        return;

                    // Does not have permission
                    if (!context.hasPermission("cps.leaderboard.fetch")) {
                        messenger.send(context, MESSAGE_NO_PERMISSION);
                        return;
                    }

                    // Fetch
                    messenger.send(context, MESSAGE_REQUEST_SENT);
                    plugin.getDataStorage().fetchBoard(perPage * page).whenComplete((board, exception) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        // If an error
                        if (board == null) {
                            messenger.send(context, MESSAGE_REQUEST_ERROR);
                            return;
                        }

                        // New indexes
                        int newPages = (int) Math.ceil((double) board.size() / perPage);
                        Function<String, String> newPageReplacer = message -> message.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages));

                        // Display
                        if (!displayBoard(context, board, perPage, page, newPages, newPageReplacer))
                            messenger.send(context, MESSAGE_PREFIX + "leaderboard.invalid-page", pageReplacer);
                    }));
                }).build());
    }

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