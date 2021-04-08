package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import com.davidcubesvk.clicksPerSecond.utils.data.DataGetResult;
import com.davidcubesvk.clicksPerSecond.utils.data.DataStorageOperator;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.WriteCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Handler for copy sub-command.
 */
public class Copy implements CommandProcessor {

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        Bukkit.getScheduler().runTaskAsynchronously(ClicksPerSecond.getPlugin(), () -> {
            //Check the permission and args
            if (!commandUtil.hasPermission("cps.command.copy") || !commandUtil.checkArgs(3, 3) || commandUtil.isFormatOutdated())
                return;

            //Check if the storage type is DATABASE
            if (ClicksPerSecond.getStorageType() != StorageType.DATABASE) {
                //Send message
                commandUtil.sendMessage("command.main.copy.databaseRequired");
                return;
            }

            //Parse saveType target and scoreboard type
            StorageType to;
            ScoreboardType scoreboardType;
            try {
                //Try to parse
                to = StorageType.valueOf(args[2].toUpperCase());
                scoreboardType = ScoreboardType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ex) {
                //Invalid format
                commandUtil.invalidFormat();
                return;
            }
            //Source storage type
            StorageType from = to == StorageType.FILE ? StorageType.DATABASE : StorageType.FILE;

            //Check if the plugin is connected to the database
            if (!Database.getInstance().isConnected()) {
                //Disconnected
                commandUtil.sendMessage("command.main.copy.error.database.disconnected",
                        message -> setPlaceholders(message, from, to));
                return;
            }

            //Getting data
            commandUtil.sendMessage("command.main.copy.phase.getting",
                    message -> setPlaceholders(message, from, to));
            //Get source scoreboard
            DataGetResult source = ClicksPerSecond.getStorageOperatorByType(from).getAllData(scoreboardType);
            //Send an error message if an error occurred
            if (notifyError(source.getResult(), commandUtil, from, to))
                return;

            //Source scoreboard
            Queue<TestRecord> sourceScoreboard = new LinkedList<>(source.getData());
            //If an empty queue
            if (sourceScoreboard.size() == 0) {
                commandUtil.sendMessage("command.main.copy.phase.nothing",
                        message -> setPlaceholders(message, from, to));
                return;
            }

            //Get the target storage operator
            DataStorageOperator targetStorageOperator = ClicksPerSecond.getStorageOperatorByType(to);
            //Write all records
            Database.OperationResult result = targetStorageOperator.writeAll(sourceScoreboard, new WriteCallback(ClicksPerSecond.getConfiguration().getInt("command.main.copy.phase.writing.delay")) {
                @Override
                public void message(long written, long total, int percent) {
                    commandUtil.sendMessage("command.main.copy.phase.writing.message",
                            message -> setPlaceholders(message, from, to)
                                    .replace("{write_percent}", "" + percent)
                                    .replace("{write_amount}", "" + written)
                                    .replace("{write_total}", "" + total));
                }
            });
            //Send an error message if an error occurred
            if (notifyError(result, commandUtil, from, to))
                return;

            //Copied
            commandUtil.sendMessage("command.main.copy.copied",
                    message -> setPlaceholders(message, from, to));
        });
    }

    /**
     * Notifies the command caller about an error (determined from the operation result) using the given command utility.
     *
     * @param result      the operation result
     * @param commandUtil the command utility
     * @param from        source data storage type
     * @param to          target data storage type
     * @return if the operation result is an error
     */
    private boolean notifyError(Database.OperationResult result, CommandUtil commandUtil, StorageType from, StorageType to) {
        switch (result) {
            case ERR_DISCONNECT:
                //Disconnected
                commandUtil.sendMessage("command.main.copy.error.database.disconnected",
                        message -> setPlaceholders(message, from, to));
                return true;
            case ERR_OTHER:
                //Send file or database error
                if (from == StorageType.FILE)
                    commandUtil.sendMessage("command.main.copy.error.file.error",
                            message -> setPlaceholders(message, from, to));
                else
                    commandUtil.sendMessage("command.main.copy.error.database.other",
                            message -> setPlaceholders(message, from, to));
                return true;
        }

        //Not a error
        return false;
    }

    /**
     * Replaces (sets) the storage_from and storage_to placeholder in a message.
     *
     * @param message the message to replace placeholders in
     * @param from    the source data storage type
     * @param to      the target data storage type
     * @return the message with replaced (set) placeholders
     */
    private String setPlaceholders(String message, StorageType from, StorageType to) {
        return message.replace("{storage_from}", from.name())
                .replace("{storage_to}", to.name());
    }

}
