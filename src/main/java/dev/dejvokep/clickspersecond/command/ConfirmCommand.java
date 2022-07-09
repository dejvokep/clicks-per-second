package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_CONFIRM_NO_PENDING;
import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_CONFIRM_REQUIRED;

public class ConfirmCommand {

    public ConfirmCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        // Confirmation manager
        CommandConfirmationManager<CommandSender> confirmationManager = new CommandConfirmationManager<>(20L, TimeUnit.SECONDS, context ->
                plugin.getMessenger().send(context.getCommandContext(), MESSAGE_CONFIRM_REQUIRED), sender -> plugin.getMessenger().send(sender, MESSAGE_CONFIRM_NO_PENDING));

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("confirm")
                .meta(CommandMeta.DESCRIPTION, "Confirms execution of a pending command.")
                .handler(confirmationManager.createConfirmationExecutionHandler()).build());
    }

}