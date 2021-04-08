package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.UUIDFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for stats sub-command.
 */
public class Stats implements CommandProcessor {

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        //Check the permission and args
        if (!commandUtil.hasPermission("cps.command.stats") || !commandUtil.checkArgs(2, 2) || commandUtil.isFormatOutdated())
            return;

        String name = null;
        UUID uuid;
        //Try to parse
        try {
            //Parse UUID
            uuid = UUIDFactory.fromString(args[1]);

            //Get the player
            OfflinePlayer offlinePlayer = (offlinePlayer = Bukkit.getPlayer(uuid)) == null ? Bukkit.getOfflinePlayer(uuid) : offlinePlayer;
            //Set the name if is offline or has played before
            if (offlinePlayer instanceof Player || offlinePlayer.hasPlayedBefore())
                name = offlinePlayer.getName();
        } catch (Exception ex) {
            //Not an UUID
            uuid = null;
        }

        //Get the UUID out of the player's name
        if (uuid == null) {
            //Get the player
            OfflinePlayer offlinePlayer = (offlinePlayer = Bukkit.getPlayerExact(args[1])) == null ? Bukkit.getOfflinePlayer(args[1]) : offlinePlayer;
            //Hasn't played before and is offline, name can not be retrieved
            if (!(offlinePlayer instanceof Player) && !offlinePlayer.hasPlayedBefore()) {
                //Not found
                commandUtil.sendMessage("command.main.stats.uuidInvalidOrNotFound",
                        message -> message.replace("{argument}", args[1]));
                return;
            }

            //Set UUID and name
            uuid = offlinePlayer.getUniqueId();
            name = args[1];
        }

        //Get API
        ClicksPerSecondAPI api = ClicksPerSecondAPI.getInstance();
        //Get records
        TestRecord right = api.getPlayerData(uuid, ScoreboardType.RIGHT);
        TestRecord left = api.getPlayerData(uuid, ScoreboardType.LEFT);
        TestRecord hack = api.getPlayerData(uuid, ScoreboardType.HACK);

        //UUID is never null, get as string
        final String player_uuid = uuid.toString();
        //Name can be null, get replacement if so
        final String player_name = name != null ? name : api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLAYER_NAME);

        //If player is not in one of the lists
        if (right == null && left == null && hack == null) {
            //No statistics
            commandUtil.sendMessage("command.main.stats.noStatistics", message ->
                    message.replace("{player_name}", player_name).replace("{player_uuid}", player_uuid));
            return;
        }

        //Send the stats message
        commandUtil.sendMessage("command.main.stats.stats", message ->
                replacePlaceholders(message, right, left, hack)
                        .replace("{player_name}", player_name).replace("{player_uuid}", player_uuid));
    }

    /**
     * Replaces all test record placeholders using {@link TestRecord#setPlaceholders(String)} (if test record is not null), otherwise, replaces with replacements.
     *
     * @param message a message to replace in
     * @param right   a record from right-click scoreboard (can be <code>null</code>)
     * @param left    a record from left-click scoreboard (can be <code>null</code>)
     * @param hack    a record from hacking scoreboard (can be <code>null</code>)
     * @return the message with replaced placeholders
     */
    private String replacePlaceholders(String message, TestRecord right, TestRecord left, TestRecord hack) {
        //API
        ClicksPerSecondAPI api = ClicksPerSecondAPI.getInstance();
        //Create map
        Map<ScoreboardType, TestRecord> testRecords = new HashMap<ScoreboardType, TestRecord>(){{
            put(ScoreboardType.RIGHT, right);
            put(ScoreboardType.LEFT, left);
            put(ScoreboardType.HACK, hack);
        }};

        //Replace for each record
        for (ScoreboardType scoreboardType : testRecords.keySet()) {
            //Construct prefix
            String prefix = scoreboardType.getName() + "_";
            //Get record
            TestRecord testRecord = testRecords.get(scoreboardType);

            //Replace if not null
            if (testRecord != null)
                message = testRecord.setPlaceholders(message, prefix);
            else
                message = message.replace("{" + prefix + "place}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLACE))
                        .replace("{" + prefix + "player_uuid}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLAYER_UUID))
                        .replace("{" + prefix + "player_name}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLAYER_NAME))
                        .replace("{" + prefix + "cps_int}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.CPS_INT))
                        .replace("{" + prefix + "cps_decimal}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.CPS_DECIMAL))
                        .replace("{" + prefix + "date}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.DATE))
                        .replace("{" + prefix + "time}", api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.TIME));
        }

        return message;
    }

}