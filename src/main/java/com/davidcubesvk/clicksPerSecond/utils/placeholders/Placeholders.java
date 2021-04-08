package com.davidcubesvk.clicksPerSecond.utils.placeholders;

import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.UUIDFactory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Class for ClicksPerSecond's PlaceholderAPI expansion.
 * Placeholder format: <code>cps_[right|left|hack]_[place|player-name|player-uuid]_[value]_[place|player-name|player-uuid|cps-int|cps-decimal|date|time]</code>
 */
public class Placeholders extends PlaceholderExpansion {

    /**
     * Enum representing all key types of the [value].
     * - PLACE: key is a place in scoreboard
     * - PLAYER_NAME: key is a player's name
     * - PLAYER_UUID: key is a player's UUID
     */
    private enum DataKey {
        PLACE, PLAYER_NAME, PLAYER_UUID
    }

    public String getIdentifier() {
        return "cps";
    }

    public String getPlugin() {
        return null;
    }

    public String getAuthor() {
        return "davidcubesvk";
    }

    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        //Split identifier into the placeholder options
        String[] identifierParts = identifier.split("_");

        //Check if the placeholder is invalid
        if (identifierParts.length != 4)
            return "";

        //Parse scoreboard, data key and placeholder type (get the replacement straight from it)
        ScoreboardType scoreboardType;
        DataKey dataKey;
        ClicksPerSecondAPI.RecordPlaceholder placeholder;
        String replacement;
        try {
            //Try to parse
            scoreboardType = ScoreboardType.valueOf(identifierParts[0].toUpperCase());
            dataKey = DataKey.valueOf(identifierParts[1].toUpperCase().replace('-', '_'));
            placeholder = ClicksPerSecondAPI.RecordPlaceholder.valueOf(identifierParts[3].toUpperCase().replace('-', '_'));

            //Get replacement
            replacement = ClicksPerSecondAPI.getInstance().getPlaceholderReplacement(placeholder);
        } catch (IllegalArgumentException ex) {
            //Return an empty string
            return "";
        }

        Object value = null;
        //Parse the place number (and check if < 1) or UUID
        try {
            switch (dataKey) {
                case PLACE:
                    value = Integer.parseInt(identifierParts[2]);
                    break;
                case PLAYER_UUID:
                    value = UUIDFactory.fromString(identifierParts[2]);
                    break;
            }
        } catch (IllegalArgumentException ex) {
            //Return an empty string
            return "";
        }

        //Get UUID from OfflinePlayer if using PLAYER_NAME
        if (dataKey == DataKey.PLAYER_NAME) {
            //Get the player
            OfflinePlayer offlinePlayer = (offlinePlayer = Bukkit.getPlayer(identifierParts[2])) == null ? Bukkit.getOfflinePlayer(identifierParts[2]) : offlinePlayer;
            //Has not played before
            if (!(offlinePlayer instanceof Player) && !offlinePlayer.hasPlayedBefore())
                return "";

            //Set value as UUID
            value = offlinePlayer.getUniqueId();
        }

        //Get the TestRecord
        TestRecord testRecord = dataKey == DataKey.PLACE ?
                ClicksPerSecondAPI.getInstance().getPlaceData((int) value, scoreboardType) :
                ClicksPerSecondAPI.getInstance().getPlayerData((UUID) value, scoreboardType);

        //If null, return an empty string
        if (testRecord == null)
            return replacement;

        //Get the property
        Object property = testRecord.getProperty(placeholder);
        //If null, return an empty string
        if (property == null)
            return replacement;

        return property.toString();
    }
}