package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.player.UUIDFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.*;

/**
 * Handler for the <code>/cps delete</code> command.
 */
public class DeleteCommand {

    // Plugin
    private final ClicksPerSecond plugin;

    /**
     * Registers the command to the given manager.
     *
     * @param plugin  the plugin
     * @param manager the manager
     */
    public DeleteCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
        this.plugin = plugin;

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("delete").permission("cps.delete")
                .argument(StringArgument.single("name|uuid|all"))
                .meta(CommandMeta.DESCRIPTION, "Deletes all or individual player data (identified by name or UUID) from the currently used data source.")
                .meta(CommandConfirmationManager.META_CONFIRMATION_REQUIRED, true).handler(context -> {
                    // The target
                    String target = context.get("name|uuid|all");

                    // Delete all
                    if (target.equals("*") || target.equals("all")) {
                        plugin.getMessenger().send(context, MESSAGE_REQUEST_SENT);
                        plugin.getDataStorage().deleteAll().whenComplete((result, exception) -> handleResult(result, context));
                        return;
                    }

                    // Parse UUID
                    UUID uuid = UUIDFactory.fromArgument(target);
                    if (uuid == null) {
                        plugin.getMessenger().send(context, MESSAGE_INVALID_NAME, message -> message.replace("{name}", target));
                        return;
                    }

                    // Delete single
                    plugin.getDataStorage().delete(uuid).whenComplete((result, exception) -> handleResult(result, context));
                }).build());
    }

    /**
     * Handles the result by sending the appropriate message to the sender of the given context.
     *
     * @param result  the result of the operation
     * @param context the command context
     */
    private void handleResult(boolean result, CommandContext<CommandSender> context) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getMessenger().send(context, result ? MESSAGE_PREFIX + "delete" : MESSAGE_REQUEST_ERROR));
    }

}