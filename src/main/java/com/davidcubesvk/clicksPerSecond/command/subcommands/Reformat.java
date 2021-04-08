package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.ReformatCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for reformat sub-command.
 */
public class Reformat implements CommandProcessor {

    //Last executions of this command
    private static final Map<UUID, Long> lastPlayerExecutions = new HashMap<>();
    private static long lastConsoleExecution = -1L;
    //If this command is already being executed
    private static boolean active = false;

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        //Check args and permission
        if (!commandUtil.checkArgs(1, 1) || !commandUtil.hasPermission("cps.command.reformat"))
            return;

        //Get reformatter
        Reformatter reformatter = Reformatter.getInstance();

        //If the version is being checked right now
        if (!reformatter.isLoaded()) {
            //Send message and return
            commandUtil.sendMessage("command.main.format.gettingVersion");
            return;
        }

        //If latest
        if (!Reformatter.getInstance().isReformatNeeded()) {
            //Send message and return
            commandUtil.sendMessage("command.main.reformat.latestFormat");
            return;
        }

        //If active already
        if (active) {
            //Send message and return
            commandUtil.sendMessage("command.main.reformat.active");
            return;
        }
        //Set to true
        active = true;

        //If running for first time, send confirmation message
        if (!runReformat(sender)) {
            //Send message
            commandUtil.sendMessage("command.main.reformat.confirmation.message");
            //Not active
            active = false;
            return;
        }

        //Run asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(ClicksPerSecond.getPlugin(), () -> {
            //Send message
            commandUtil.sendMessage("command.main.reformat.phase.rechecking");
            //Re-check the format version
            reformatter.refreshFormatVersion(false);

            //If latest
            if (!reformatter.isReformatNeeded()) {
                //Send message and return
                commandUtil.sendMessage("command.main.reformat.latestFormat");
                return;
            }

            //Reformat
            ClicksPerSecond.getStorageOperatorByType(ClicksPerSecond.getStorageType()).reformat(new ReformatCallback(
                    //Get the message delay
                    ClicksPerSecond.getConfiguration().getInt(ClicksPerSecond.getStorageType() == StorageType.DATABASE ? "command.main.reformat.phase.database.writing.delay" : "command.main.reformat.phase.file.converting.delay")
            ) {

                @Override
                public void PHASE_getting(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.getting", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void PHASE_skipping(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.skipping", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void PHASE_reformatting(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.reformatting", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void PHASE_finished(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.finished", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void PHASE_finished() {
                    commandUtil.sendMessage("command.main.reformat.finished");
                }

                @Override
                public void PHASE_resumed() {
                    commandUtil.sendMessage("command.main.reformat.phase.resumed");
                }

                @Override
                public void PHASE_DATABASE_recreatingTable(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.database.recreatingTable", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void PHASE_DATABASE_writing(ScoreboardType scoreboardType, long written, long total, int percent) {
                    commandUtil.sendMessage("command.main.reformat.phase.database.writing.message", message ->
                            message.replace("{scoreboard}", scoreboardType.name())
                                    .replace("{write_amount}", "" + written)
                                    .replace("{write_total}", "" + total)
                                    .replace("{write_percent}", "" + percent));
                }

                @Override
                public void PHASE_FILE_converting(ScoreboardType scoreboardType, long converted, long total, int percent) {
                    commandUtil.sendMessage("command.main.reformat.phase.file.converting.message", message ->
                            message.replace("{scoreboard}", scoreboardType.name())
                                    .replace("{convert_amount}", "" + converted)
                                    .replace("{convert_total}", "" + total)
                                    .replace("{convert_percent}", "" + percent));
                }

                @Override
                public void PHASE_FILE_writing(ScoreboardType scoreboardType) {
                    commandUtil.sendMessage("command.main.reformat.phase.file.writing", message -> message.replace("{scoreboard}", scoreboardType.name()));
                }

                @Override
                public void ERROR_DATABASE_disconnected() {
                    commandUtil.sendMessage("command.main.reformat.error.database.disconnected");
                }

                @Override
                public void ERROR_DATABASE_other() {
                    commandUtil.sendMessage("command.main.reformat.error.database.other");
                }

                @Override
                public void ERROR_DATABASE_POST_restart() {
                    commandUtil.sendMessage("command.main.reformat.error.database.post.restart");
                }

                @Override
                public void ERROR_DATABASE_POST_resume() {
                    commandUtil.sendMessage("command.main.reformat.error.database.post.resume");
                }

                @Override
                public void ERROR_DATABASE_POST_retryWait() {
                    commandUtil.sendMessage("command.main.reformat.error.database.post.retryWait");
                }

                @Override
                public void ERROR_FILE_error() {
                    commandUtil.sendMessage("command.main.reformat.error.file.error");
                }
            });

            //Not active
            active = false;
        });
    }

    /**
     * Returns if the command sender is running this command again to confirm (within the configured limit) the start of reformatting process.
     * <p></p>
     * If running for first time, saves the current UNIX time (to determine if the second command execution is within the limit), if reformatting process was confirmed, clears it from the local {@link Map}.
     *
     * @param sender the command sender
     * @return if the command sender has confirmed to start the reformatting process
     */
    private boolean runReformat(CommandSender sender) {
        //Time when this command was executed last time
        long lastExecution;

        //Get by UUID or console
        if (sender instanceof Player)
            lastExecution = lastPlayerExecutions.getOrDefault(((Player) sender).getUniqueId(), -1L);
        else
            lastExecution = lastConsoleExecution;

        //If running reformatting process
        long now = System.currentTimeMillis();
        boolean reformat = lastExecution != -1 && lastExecution + ClicksPerSecond.getConfiguration().getLong("command.main.reformat.confirmation.timeout") > now;

        //Just sending warning message
        if (!reformat) {
            //Set the time
            if (sender instanceof Player)
                lastPlayerExecutions.put(((Player) sender).getUniqueId(), now);
            else
                lastConsoleExecution = now;
        } else {
            //Clear the time
            if (sender instanceof Player)
                lastPlayerExecutions.remove(((Player) sender).getUniqueId());
            else
                lastConsoleExecution = -1L;
        }

        //Return
        return reformat;
    }

}