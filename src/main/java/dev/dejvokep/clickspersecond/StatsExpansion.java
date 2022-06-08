package dev.dejvokep.clickspersecond;

import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class StatsExpansion extends PlaceholderExpansion {

    private final ClicksPerSecond plugin;
    private String unknownValue;
    private SimpleDateFormat dateFormat;

    public StatsExpansion(ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.unknownValue = plugin.getConfiguration().getString("placeholder.unknown-value");
        this.dateFormat = new SimpleDateFormat(plugin.getConfiguration().getString("placeholder.date-format"));
    }

    @Override
    public @Nullable
    String onRequest(OfflinePlayer player, @NotNull String params) {
        // To lower case
        params = params.toLowerCase();

        // Requesting current CPS
        if (params.equals("now"))
            return player.isOnline() ? convertToUnknown(plugin.getClickHandler().getCPS((Player) player), -1) : unknownValue;

        // Requesting best CPS
        if (params.startsWith("best")) {
            PlayerInfo info = plugin.getClickHandler().getInfo(player.getUniqueId());
            if (info == null || info.isLoading())
                return unknownValue;

            if (params.equals("best_cps"))
                return String.valueOf(info.getCPS());
            if (params.equals("best_date_millis"))
                return String.valueOf(info.getTime());
            if (params.equals("best_date_formatted"))
                return dateFormat.format(new Date(info.getTime()));
            return unknownValue;
        }

        // Requesting leaderboard
        if (params.startsWith("leaderboard")) {
            // Data
            String[] identifiers = params.split("_");
            List<PlayerInfo> leaderboard = plugin.getDataStorage().getLeaderboard();

            // Insufficient length
            if (identifiers.length < 3 || identifiers.length > 4)
                return unknownValue;

            // Parse place
            int place;
            try {
                place = Integer.parseInt(identifiers[1]);
            } catch (NumberFormatException ignored) {
                return unknownValue;
            }

            // Unknown place
            if (place < 1 || place > leaderboard.size())
                return unknownValue;

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
                        return dateFormat.format(new Date(info.getTime()));
                    default:
                        return unknownValue;
                }
            }

            // If the 3rd one is date
            if (requested.equalsIgnoreCase("date")) {
                // Return by selector
                if (identifiers[3].equalsIgnoreCase("millis"))
                    return String.valueOf(info.getCPS());
                else if (identifiers[3].equalsIgnoreCase("formatted"))
                    return dateFormat.format(new Date(info.getTime()));
            }
        }

        // Unknown request
        return unknownValue;
    }

    private <T> String convertToUnknown(@Nullable T value, @NotNull T condition) {
        return value == null || value.equals(condition) ? unknownValue : value.toString();
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