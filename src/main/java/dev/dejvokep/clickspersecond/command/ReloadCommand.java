/*
 * Copyright 2022 https://dejvokep.dev/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dejvokep.clickspersecond.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.meta.CommandMeta;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.display.Display;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;

import static dev.dejvokep.clickspersecond.utils.messaging.Messenger.MESSAGE_PREFIX;

/**
 * Handler for the <code>/cps reload</code> command.
 */
public class ReloadCommand {

    /**
     * Registers the command to the given manager.
     *
     * @param plugin  the plugin
     * @param manager the manager
     */
    public ReloadCommand(@NotNull ClicksPerSecond plugin, @NotNull CommandManager<CommandSender> manager) {
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
                    plugin.getListeners().reload();

                    // Success
                    plugin.getMessenger().send(context, MESSAGE_PREFIX + "reload");
                }).build());
    }

}