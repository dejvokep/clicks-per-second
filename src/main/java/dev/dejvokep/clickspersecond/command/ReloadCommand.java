package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.logging.Level;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_PREFIX;

public class ReloadCommand {

    public ReloadCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        // Register
        manager.command(manager.commandBuilder("cps", "clickspersecond").literal("reload").permission("cps.reload")
                .meta(CommandMeta.DESCRIPTION, "Reloads the plugin.")
                .handler(context -> {
                    // Reload
                    try {
                        plugin.getConfiguration().reload();
                    } catch (IOException ex) {
                        plugin.getLogger().log(Level.SEVERE, "An error occurred whilst reloading plugin configuration!", ex);
                    }
                    plugin.getDataStorage().reload();
                    plugin.getPlaceholderReplacer().reload();
                    plugin.getDisplays().forEach(Display::reload);

                    // Success
                    plugin.getMessenger().send(context, MESSAGE_PREFIX + "reload");
                }).build());
    }

}