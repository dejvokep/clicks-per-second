package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

public class ConfirmCommand extends PluginCommand {

    public ConfirmCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        super(plugin);

        // Confirmation manager
        CommandConfirmationManager<CommandSender> confirmationManager = new CommandConfirmationManager<>(20L, TimeUnit.SECONDS, context ->
                send(context.getCommandContext(), MESSAGE_CONFIRM_REQUIRED), sender -> send(sender, MESSAGE_CONFIRM_NO_PENDING));

        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("confirm").handler(confirmationManager.createConfirmationExecutionHandler()).build());
    }

}