package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.context.CommandContext;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class PluginCommand {

    public static final String MESSAGE_PREFIX = "command.";

    public static final String MESSAGE_REQUEST_PENDING = MESSAGE_PREFIX + "data-operation.pending";
    public static final String MESSAGE_REQUEST_SENT = MESSAGE_PREFIX + "data-operation.sent";
    public static final String MESSAGE_REQUEST_ERROR = MESSAGE_PREFIX + "data-operation.error";

    private final ClicksPerSecond plugin;

    public PluginCommand(ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    public void send(CommandContext<CommandSender> context, String messageId) {
        send(context, messageId, null);
    }

    public void send(CommandContext<CommandSender> context, String messageId, Function<String, String> replacer) {
        if (context.getSender() instanceof Player && !((Player) context.getSender()).isOnline())
            return;
        String message = plugin.getConfiguration().getString(messageId);
        context.getSender().sendMessage(replacer == null ? message : replacer.apply(message));
    }

}