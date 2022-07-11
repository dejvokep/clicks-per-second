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
package dev.dejvokep.clickspersecond.utils.placeholders;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * A (partly) utility class used to replace placeholders. There are 4 groups of placeholders that can be replaced:
 * <ol>
 *     <li>
 *         <b>Player</b>
 *         <ul>
 *             <li><code>{uuid}</code> - player's {@link UUID}</li>
 *             <li><code>{name}</code> - player's name, or {@link #getUnknownValue() unknown} if unavailable (player has not played on the server before)</li>
 *             <li><code>{id}</code> - player's name, or {@link UUID} if unavailable (player has not played on the server before)</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <b>Info</b>
 *         <ul>
 *             <li><code>{cps_best}</code> - the best CPS</li>
 *             <li><code>{cps_best_date}</code> - formatted date (according to {@link #getDateFormat()}) at which the best CPS were achieved (or {@link #getUnknownValue() unknown} if {@link PlayerInfo#isEmpty()})</li>
 *             <li><code>{cps_best_date_millis}</code> - date in millis at which the best CPS were achieved (or {@link #getUnknownValue() unknown} if {@link PlayerInfo#isEmpty()})</li>
 *             <li><code>{cps_best_date_formatted}</code> - alias for <code>{cps_best_date}</code></li>
 *             <li>+ placeholders from player group</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <b>All</b>
 *         <ul>
 *             <li><code>{cps_now}</code> - current CPS</li>
 *             <li>+ placeholders from player and info groups</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <b>API</b>
 *         <ul>
 *             <li>all PlaceholderAPI placeholders</li>
 *             <li>+ placeholders from group all</li>
 *         </ul>
 *     </li>
 * </ol>
 */
public class PlaceholderReplacer {

    /**
     * Represents whether PlaceholderAPI is available and can be used.
     */
    public static final boolean PLACEHOLDER_API_AVAILABLE = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    // Plugin
    private final ClicksPerSecond plugin;

    // Values
    private String unknownValue;
    private SimpleDateFormat dateFormat;
    private boolean allowPlaceholderApi;

    /**
     * Initializes the replacer, automatically calls {@link #reload()}.
     *
     * @param plugin the plugin
     */
    public PlaceholderReplacer(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Replaces placeholders from the player group in the given message.
     *
     * @param uuid    the ID to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String player(@NotNull UUID uuid, @NotNull String message) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return message.replace("{uuid}", uuid.toString())
                .replace("{name}", player.getName() == null ? unknownValue : player.getName())
                .replace("{id}", player.getName() == null ? uuid.toString() : player.getName());
    }

    /**
     * Replaces placeholders from the player group in the given message.
     *
     * @param player  the player to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String player(@NotNull Player player, @NotNull String message) {
        return message.replace("{uuid}", player.getUniqueId().toString())
                .replace("{name}", player.getName())
                .replace("{id}", player.getName());
    }

    /**
     * Replaces placeholders from the info group in the given message.
     *
     * @param info    the info to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String info(@NotNull PlayerInfo info, @NotNull String message) {
        message = player(info.getUniqueId(), message).replace("{cps_best}", String.valueOf(info.getCPS()));
        if (info.isEmpty())
            return message;

        String formattedDate = dateFormat.format(new Date(info.getTime()));
        return message.replace("{cps_best_date}", formattedDate)
                .replace("{cps_best_date_millis}", String.valueOf(info.getTime()))
                .replace("{cps_best_date_formatted}", formattedDate);
    }

    /**
     * Replaces placeholders from the all group in the given message.
     *
     * @param sampler the sampler to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String all(@NotNull Sampler sampler, @NotNull String message) {
        return info(sampler.getInfo(), message).replace("{cps_now}", String.valueOf(sampler.getCPS()));
    }

    /**
     * Replaces placeholders from the all group in the given message.
     *
     * @param player  the player to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String all(@NotNull Player player, @NotNull String message) {
        return all(Objects.requireNonNull(plugin.getClickHandler().getSampler(player.getUniqueId())), message);
    }

    /**
     * Replaces placeholders from the API group in the given message. Note that the use of PlaceholderAPI is subject to
     * its enabled state and {@link #allowPlaceholderApi}.
     *
     * @param player  the player to replace for
     * @param message the message
     * @return the message with replaced placeholders
     */
    @NotNull
    public String api(@NotNull Player player, @NotNull String message) {
        message = all(Objects.requireNonNull(plugin.getClickHandler().getSampler(player.getUniqueId())), message);
        return allowPlaceholderApi && PLACEHOLDER_API_AVAILABLE ? PlaceholderAPI.setPlaceholders(player, message) : message;
    }

    /**
     * Reloads the internal configuration.
     */
    public void reload() {
        Section config = plugin.getConfiguration().getSection("placeholder");
        this.unknownValue = config.getString("unknown-value");
        this.dateFormat = new SimpleDateFormat(config.getString("date-format"));
        this.allowPlaceholderApi = config.getBoolean("allow-placeholder-api");
    }

    /**
     * Returns the date format.
     *
     * @return the date format
     */
    @NotNull
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Returns the replacement for unknown values.
     *
     * @return the replacement for unknown values
     */
    @NotNull
    public String getUnknownValue() {
        return unknownValue;
    }

}