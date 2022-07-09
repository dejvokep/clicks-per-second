package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.UUIDFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.UUID;

import static dev.dejvokep.clickspersecond.Messenger.*;

public class DeleteCommand {

    private final ClicksPerSecond plugin;

    public DeleteCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        this.plugin = plugin;

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("delete").permission("cps.delete")
                .argument(StringArgument.single("target"))
                .meta(CommandMeta.DESCRIPTION, "Deletes all or individual player data (identified by name or UUID) from the currently used data source.")
                .meta(CommandConfirmationManager.META_CONFIRMATION_REQUIRED, true).handler(context -> {
                    // The target
                    String target = context.get("target");

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

    private void handleResult(boolean result, CommandContext<CommandSender> context) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // If an error
            if (!result) {
                plugin.getMessenger().send(context, MESSAGE_REQUEST_ERROR);
                return;
            }

            // Success
            plugin.getMessenger().send(context, MESSAGE_PREFIX + "delete");
        });
    }

}