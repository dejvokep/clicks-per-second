package com.davidcubesvk.clicksPerSecond.utils.data.database;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.ReformatCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.WriteCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.Queue;
import java.util.logging.Level;

/**
 * Re-runs the reformatting process if an error occurs.
 */
class ReformatFailPostExecutor {

    //Post-error retry waiting timeout
    private static final int POST_ERROR_RETRY_WAIT_TICKS = 100;

    /**
     * Enum representing all possible data-lose-risky phases where the reformatting process could've failed (when using database).
     * - DROP: failed when dropping a table
     * - CREATE: failed when creating a table
     * - WRITE: failed during the writing process
     * - NONE: no fail
     */
    enum FailPhase {
        DROP(0), CREATE(1), WRITE(2), NONE(-1);

        //Phase ID
        private int phaseId;

        /**
         * Initializes the constant by the given phase ID.
         *
         * @param phaseId the phase ID (incremental)
         */
        FailPhase(int phaseId) {
            this.phaseId = phaseId;
        }

        /**
         * Returns an enum constant by the given phase ID.
         *
         * @param phaseId the phase ID
         * @return the enum constant corresponding to the given phase ID
         */
        public static FailPhase getById(int phaseId) {
            //Return by ID
            switch (phaseId) {
                case -1:
                    return FailPhase.NONE;
                case 0:
                    return FailPhase.DROP;
                case 1:
                    return FailPhase.CREATE;
                case 2:
                    return FailPhase.WRITE;
                default:
                    return null;
            }
        }
    }

    //Scoreboard type during which's the reformatting failed
    private ScoreboardType failedScoreboard;
    //Reformat data queue
    private Queue<TestRecord> reformatQueue;
    //Stage in which the process failed
    private FailPhase failPhase;
    //Callback
    private ReformatCallback reformatCallback;

    /**
     * Initializes this post executor by the given data.
     *
     * @param scoreboardType   the scoreboard type which was being reformatted when the process failed
     * @param reformatQueue    a queue containing all reformatted records
     * @param failPhase        a constant representing a phase when the process failed
     * @param reformatCallback a callback to reuse for post-execution
     */
    ReformatFailPostExecutor(ScoreboardType scoreboardType, Queue<TestRecord> reformatQueue, FailPhase failPhase, ReformatCallback reformatCallback) {
        this.failedScoreboard = scoreboardType;
        this.reformatQueue = reformatQueue;
        this.failPhase = failPhase;
        this.reformatCallback = reformatCallback;
    }

    /**
     * Executes the post-reformatting process (process part that has not been executed because an error had occurred before it).
     * This method must be called asynchronously.
     */
    void execute() {
        try {
            //Return if the process did not fail in any stage
            if (failPhase == FailPhase.NONE)
                return;

            //Process is active
            Database.getInstance().reformatActive = true;
            //Send message
            reformatCallback.PHASE_resumed();

            //Copy the current fail stage
            FailPhase before = FailPhase.valueOf(failPhase.name());

            //Failed in table-drop stage
            if (failPhase.phaseId == 0) {
                //Send message
                reformatCallback.PHASE_DATABASE_recreatingTable(failedScoreboard);
                //Drop table
                Database.getInstance().dropTable(failedScoreboard);
                //Set to next possible failure point
                failPhase = FailPhase.CREATE;
            }

            //Failed in table-create stage
            if (failPhase.phaseId <= 1) {
                //Send message if this is the first operation (table was not dropped before in the code above)
                if (before.phaseId == 1)
                    reformatCallback.PHASE_DATABASE_recreatingTable(failedScoreboard);

                //Create the table
                Database.getInstance().createTable(failedScoreboard);
                //Set to next possible failure point
                failPhase = FailPhase.WRITE;
            }

            //Failed in write stage
            if (failPhase.phaseId <= 2) {
                //Content size
                int size = reformatQueue.size();
                //Operation result
                Database.OperationResult operationResult = Database.OperationResult.SUCCESS;

                //If empty scoreboard
                if (size == 0) {
                    //Call to inform the sender
                    reformatCallback.PHASE_skipping(failedScoreboard);
                } else {
                    //Write all
                    operationResult = Database.getInstance().writeAll(reformatQueue, new WriteCallback(reformatCallback.getMessageDelay()) {
                        @Override
                        public void message(long written, long total, int percent) {
                            reformatCallback.PHASE_DATABASE_writing(failedScoreboard, written, total, percent);
                        }
                    }, true);
                }

                //If the writing succeeded
                if (operationResult == Database.OperationResult.SUCCESS) {
                    //Everything completed, no failure
                    failPhase = FailPhase.NONE;
                    //Mark scoreboard as reformatted
                    Reformatter.getInstance().updateVersion(failedScoreboard);
                    //Finished reformatting this scoreboard
                    reformatCallback.PHASE_finished(failedScoreboard);

                    //If no other scoreboard needs to be reformatted
                    if (Reformatter.getInstance().getToReformat().size() == 0) {
                        //Finished
                        reformatCallback.PHASE_finished();
                    } else {
                        //Reformat the remaining scoreboards
                        Database.getInstance().reformat(reformatCallback);
                    }
                } else {
                    //If disconnected
                    boolean disconnected = operationResult == Database.OperationResult.ERR_DISCONNECT;
                    //Run by exception
                    mainErrorMessage(disconnected, null, reformatCallback);
                    postErrorMessage(disconnected, reformatCallback);
                }
            }
        } catch (SQLException ex) {
            //Check the exception
            boolean disconnected = Database.getInstance().checkException(ex);
            //Run by exception
            mainErrorMessage(disconnected, ex, reformatCallback);
            postErrorMessage(disconnected, reformatCallback);
        }

        //Process is not active
        Database.getInstance().reformatActive = false;
    }

    /**
     * Sends main error messages (disconnected, other) to the process caller about an exception (created during the reformatting process) using the given callback.
     *
     * @param disconnected     if the error was caused by disconnection
     * @param ex               optional exception instance to log (if you don't want to log it, leave it as <code>null</code>)
     * @param reformatCallback the callback
     * @return if the error was caused by disconnection
     */
    static boolean mainErrorMessage(boolean disconnected, Exception ex, ReformatCallback reformatCallback) {
        if (disconnected) {
            //Disconnected
            reformatCallback.ERROR_DATABASE_disconnected();
            //Log
            logConsoleMessage("An error occurred during the reformatting process (ERR_DISCONNECT)! Resuming when reconnected.", ex);
            //Return
            return true;
        }

        //Log
        logConsoleMessage("An error occurred during the reformatting process (ERR_OTHER)! Trying again in 5 seconds.", ex);
        reformatCallback.ERROR_DATABASE_other();
        //Return
        return false;
    }

    /**
     * Logs the given message with the given exception (if <code>null</code>, logs without) into the console.
     *
     * @param message the message
     * @param ex      the exception instance
     */
    private static void logConsoleMessage(String message, Exception ex) {
        //If the exception is null
        if (ex == null)
            //Log without it
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, message);
        else
            //Log it
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, message, ex);
    }

    /**
     * Sends data-risky phase's post-error messages (resume, retryWait) to the process caller using the given callback.
     * If the error has not been caused by lost connection, calls {@link #execute()} in 5 seconds. Method {@link #mainErrorMessage(boolean, Exception, ReformatCallback)} should be called before this one.
     *
     * @param disconnected     if the error was caused by disconnection
     * @param reformatCallback the callback
     */
    void postErrorMessage(boolean disconnected, ReformatCallback reformatCallback) {
        if (disconnected) {
            //The operation will be resumed when reconnected
            reformatCallback.ERROR_DATABASE_POST_resume();
        } else {
            //Waiting for 5 seconds
            reformatCallback.ERROR_DATABASE_POST_retryWait();
            //Run again in 5 seconds
            Bukkit.getScheduler().runTaskLaterAsynchronously(ClicksPerSecond.getPlugin(), this::execute, POST_ERROR_RETRY_WAIT_TICKS);
        }
    }

}
