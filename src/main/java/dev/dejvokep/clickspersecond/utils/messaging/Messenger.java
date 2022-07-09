package dev.dejvokep.clickspersecond.utils.messaging;

import cloud.commandframework.context.CommandContext;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public Messenger(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
    }

    public void send(@NotNull CommandContext<CommandSender> context, @NotNull String messageId) {
        send(context, messageId, null);
    }

    public void send(@NotNull CommandSender sender, @NotNull String messageId) {
        send(sender, messageId, null);
    }

    public void send(@NotNull CommandContext<CommandSender> context, @NotNull String messageId, @Nullable Function<String, String> replacer) {
        send(context.getSender(), messageId, replacer);
    }

    public void send(@NotNull CommandSender sender, @NotNull String messageId, @Nullable Function<String, String> replacer) {
        if (sender instanceof Player && !((Player) sender).isOnline())
            return;
        String message = plugin.getConfiguration().getString(messageId);
        sender.sendMessage(replacer == null ? message : replacer.apply(message));
    }

}