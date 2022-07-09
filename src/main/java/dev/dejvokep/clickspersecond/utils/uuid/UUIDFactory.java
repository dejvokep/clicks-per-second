package dev.dejvokep.clickspersecond.utils.uuid;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Pattern;

public class UUIDFactory {

    /**
     * Pattern for matching and converting to UUID with dashes.
     */
    private static final Pattern UUID_DASH_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    @Nullable
    public static UUID fromString(@NotNull String uuid) {
        return uuid.length() == 36 ? UUID.fromString(uuid) : uuid.length() == 32 ? UUID.fromString(UUID_DASH_PATTERN.matcher(uuid).replaceAll("$1-$2-$3-$4-$5")) : null;
    }

    @Nullable
    public static UUID fromArgument(@NotNull String argument) {
        // If a UUID
        if (argument.length() == 32 || argument.length() == 36)
            // Parse
            return fromString(argument);


        // Player
        OfflinePlayer player = Bukkit.getOfflinePlayer(argument);
        return player.hasPlayedBefore() ? player.getUniqueId() : null;
    }

}