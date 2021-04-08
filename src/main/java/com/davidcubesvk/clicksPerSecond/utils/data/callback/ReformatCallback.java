package com.davidcubesvk.clicksPerSecond.utils.data.callback;

import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;

/**
 * Callback for reformatting process.
 */
public abstract class ReformatCallback {

    //Message delay
    private int messageDelay;

    /**
     * Initializes the messaging delay.
     *
     * @param messageDelay the messaging delay
     */
    protected ReformatCallback(int messageDelay) {
        this.messageDelay = messageDelay;
    }

    /**
     * Returns the delay associated with this callback messenger.
     *
     * @return the delay set in constructor of this instance
     */
    public int getMessageDelay() {
        return messageDelay;
    }

    //Phase methods

    /**
     * Called on the beginning of the reformatting process, when scoreboard's data are being obtained.
     *
     * @param scoreboardType type of the scoreboard that is being obtained
     */
    public abstract void PHASE_getting(ScoreboardType scoreboardType);

    /**
     * Called when a scoreboard is being skipped, because it has no data to be reformatted.
     *
     * @param scoreboardType type of the skipped scoreboard
     */
    public abstract void PHASE_skipping(ScoreboardType scoreboardType);

    /**
     * Called when reformatting process has started for a scoreboard.
     *
     * @param scoreboardType type of the scoreboard for which has the reformatting process started
     */
    public abstract void PHASE_reformatting(ScoreboardType scoreboardType);

    /**
     * Called when the reformatting process has finished for a scoreboard.
     *
     * @param scoreboardType type of the scoreboard which has been reformatted
     */
    public abstract void PHASE_finished(ScoreboardType scoreboardType);

    /**
     * Called when the reformatting process has finished completely.
     */
    public abstract void PHASE_finished();

    /**
     * Called when the reformatting process has been resumed.
     */
    public abstract void PHASE_resumed();

    //Database-sided methods

    /**
     * Called when recreating a database table to have the latest format.
     *
     * @param scoreboardType type of the scoreboard corresponding with the database table that's being recreated
     */
    public abstract void PHASE_DATABASE_recreatingTable(ScoreboardType scoreboardType);

    /**
     * Called per set delay to notify the command sender about the reformatting process status during the writing phase (if reformatting a <code>DATABASE</code> scoreboard).
     *
     * @param scoreboardType type of the scoreboard which is being reformatted
     * @param written        amount of records written
     * @param total          total amount of records to be written
     * @param percent        indicates in percent how many records have been written against the total amount
     */
    public abstract void PHASE_DATABASE_writing(ScoreboardType scoreboardType, long written, long total, int percent);

    //File-sided methods

    /**
     * Called per set delay to notify the command sender about the reformatting process status during the converting phase (if reformatting an <code>FILE</code> scoreboard).
     * <p></p>
     * During this phase, a test record scoreboard is converted into a string scoreboard.
     *
     * @param scoreboardType type of the scoreboard which is being converted
     * @param converted      amount of records converted
     * @param total          total amount of records to be converted
     * @param percent        indicates in percent how many records have been converted against the total amount
     */
    public abstract void PHASE_FILE_converting(ScoreboardType scoreboardType, long converted, long total, int percent);

    /**
     * Called when a scoreboard in the string list format is being written into the scoreboard.yml file (if reformatting an <code>FILE</code> scoreboard).
     *
     * @param scoreboardType type of the scoreboard which is being written
     */
    public abstract void PHASE_FILE_writing(ScoreboardType scoreboardType);

    //Error methods
    //Database-sided methods

    /**
     * Called when the plugin has lost connection to the database.
     */
    public abstract void ERROR_DATABASE_disconnected();

    /**
     * Called when an other (not caused by disconnection) error occurred during the reformatting process.
     */
    public abstract void ERROR_DATABASE_other();

    //Post-error methods

    /**
     * Called if the result of an error is that process must be restarted.
     */
    public abstract void ERROR_DATABASE_POST_restart();

    /**
     * Called if the process will be resumed after the plugin gets reconnected to the database.
     */
    public abstract void ERROR_DATABASE_POST_resume();

    /**
     * Called if the result of an error is that process will resume and retry the last operation in 5 seconds.
     */
    public abstract void ERROR_DATABASE_POST_retryWait();

    //File-sided methods

    /**
     * Called when a error occurred during the reformatting process.
     */
    public abstract void ERROR_FILE_error();
}