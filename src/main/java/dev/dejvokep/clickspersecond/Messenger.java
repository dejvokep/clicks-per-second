package dev.dejvokep.clickspersecond;

import cloud.commandframework.context.CommandContext;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class Messenger {

    public static final String MESSAGE_PREFIX = "messages.";

    public static final String MESSAGE_NO_PERMISSION = MESSAGE_PREFIX + "no-permission";
    public static final String MESSAGE_PLAYERS_ONLY = MESSAGE_PREFIX + "players-only";
    public static final String MESSAGE_INVALID_NAME = MESSAGE_PREFIX + "invalid-name";
    public static final String MESSAGE_CONFIRM_REQUIRED = MESSAGE_PREFIX + "confirm.required";
    public static final String MESSAGE_CONFIRM_NO_PENDING = MESSAGE_PREFIX + "confirm.not-pending";
    public static final String MESSAGE_REQUEST_PENDING = MESSAGE_PREFIX + "data-operation.pending";
    public static final String MESSAGE_REQUEST_SENT = MESSAGE_PREFIX + "data-operation.sent";
    public static final String MESSAGE_REQUEST_ERROR = MESSAGE_PREFIX + "data-operation.error";

    private final ClicksPerSecond plugin;

    public Messenger(ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    public void send(CommandContext<CommandSender> context, String messageId) {
        send(context, messageId, null);
    }

    public void send(CommandSender sender, String messageId) {
        send(sender, messageId, null);
    }

    public void send(CommandContext<CommandSender> context, String messageId, Function<String, String> replacer) {
        send(context.getSender(), messageId, replacer);
    }

    public void send(CommandSender sender, String messageId, Function<String, String> replacer) {
        if (sender instanceof Player && !((Player) sender).isOnline())
            return;
        String message = plugin.getConfiguration().getString(messageId);
        sender.sendMessage(replacer == null ? message : replacer.apply(message));
    }

    public ClicksPerSecond getPlugin() {
        return plugin;
    }
}