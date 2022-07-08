package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.UUIDFactory;
import dev.dejvokep.clickspersecond.display.Display;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class ReloadCommand extends PluginCommand {

    public ReloadCommand(ClicksPerSecond plugin, CommandManager<CommandSender> manager) {
        super(plugin);

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
                    send(context, MESSAGE_PREFIX + "reload");
                }).build());
    }

}