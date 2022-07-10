package dev.dejvokep.clickspersecond.utils.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A utility class for UUID parsing.
 */
public class UUIDFactory {

    /**
     * Pattern for matching and converting to UUID with dashes.
     */
    private static final Pattern UUID_DASH_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    /**
     * Parses a {@link UUID unique ID} from the given string. If it cannot be parsed (is invalid, or the string is not
     * exactly 32 or 36 characters long), returns <code>null</code>.
     *
     * @param uuid the ID to parse
     * @return the parsed UUID
     */
    @Nullable
    public static UUID fromString(@NotNull String uuid) {
        try {
            return uuid.length() == 36 ? UUID.fromString(uuid) : uuid.length() == 32 ? UUID.fromString(UUID_DASH_PATTERN.matcher(uuid).replaceAll("$1-$2-$3-$4-$5")) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Parses a {@link UUID unique ID} from the given command argument. This method not only accepts IDs as described by
     * {@link #fromString(String)}, but also player names, which it attempts to convert to IDs.
     * <p>
     * If the string is not a valid ID and a player could not be found by the name, returns <code>null</code>.
     *
     * @param argument the argument to parse
     * @return the parsed UUID
     */
    @Nullable
    public static UUID fromArgument(@NotNull String argument) {
        // If a UUID
        if (argument.length() == 32 || argument.length() == 36)
            // Parse
            return fromString(argument);


        // Player
        OfflinePlayer player = Bukkit.getOfflinePlayer(argument);
        return player.isOnline() || player.hasPlayedBefore() ? player.getUniqueId() : null;
    }

}