package dev.dejvokep.clickspersecond.utils.placeholders;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class StatsExpansion extends PlaceholderExpansion {

    private final ClicksPerSecond plugin;
    private final PlaceholderReplacer replacer;

    public StatsExpansion(ClicksPerSecond plugin) {
        this.plugin = plugin;
        this.replacer = plugin.getPlaceholderReplacer();
    }

    @Override
    public @Nullable
    String onRequest(OfflinePlayer player, @NotNull String params) {
        // To lower case
        params = params.toLowerCase();

        // Requesting current CPS
        if (params.equals("now"))
            return player.isOnline() ? convertToUnknown(plugin.getClickHandler().getCPS((Player) player), -1) : replacer.getUnknownValue();

        // Requesting best CPS
        if (params.startsWith("best")) {
            PlayerInfo info = plugin.getClickHandler().getInfo(player.getUniqueId());
            if (info.isLoading())
                return replacer.getUnknownValue();

            if (params.equals("best_cps"))
                return String.valueOf(info.getCPS());
            if (params.equals("best_date_millis"))
                return String.valueOf(info.getTime());
            if (params.equals("best_date_formatted"))
                return replacer.getDateFormat().format(new Date(info.getTime()));
            return replacer.getUnknownValue();
        }

        // Requesting leaderboard
        if (params.startsWith("leaderboard")) {
            // Data
            String[] identifiers = params.split("_");
            List<PlayerInfo> leaderboard = plugin.getDataStorage().getLeaderboard();

            // Insufficient length
            if (identifiers.length < 3 || identifiers.length > 4)
                return replacer.getUnknownValue();

            // Parse place
            int place;
            try {
                place = Integer.parseInt(identifiers[1]);
            } catch (NumberFormatException ignored) {
                return replacer.getUnknownValue();
            }

            // Unknown place
            if (place < 1 || place > leaderboard.size())
                return replacer.getUnknownValue();

            // Info
            PlayerInfo info = leaderboard.get(place - 1);

            // Return by requested
            String requested = identifiers[2].toLowerCase();

            // If there are only 3 identifiers
            if (identifiers.length == 3) {
                switch (requested) {
                    case "cps":
                        return String.valueOf(info.getCPS());
                    case "uuid":
                        return info.getUniqueId().toString();
                    case "name":
                        return convertToUnknown(Bukkit.getOfflinePlayer(info.getUniqueId()).getName(), 0);
                    case "date":
                        return replacer.getDateFormat().format(new Date(info.getTime()));
                    default:
                        return replacer.getUnknownValue();
                }
            }

            // If the 3rd one is date
            if (requested.equalsIgnoreCase("date")) {
                // Return by selector
                if (identifiers[3].equalsIgnoreCase("millis"))
                    return String.valueOf(info.getCPS());
                else if (identifiers[3].equalsIgnoreCase("formatted"))
                    return replacer.getDateFormat().format(new Date(info.getTime()));
            }
        }

        // Unknown request
        return replacer.getUnknownValue();
    }

    private <T> String convertToUnknown(@Nullable T value, @NotNull T condition) {
        return value == null || value.equals(condition) ? replacer.getUnknownValue() : value.toString();
    }

    @Override
    public @NotNull
    String getIdentifier() {
        return "cps";
    }

    @Override
    public @NotNull
    String getAuthor() {
        return "dejvokep";
    }

    @Override
    public @NotNull
    String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }
}