package com.davidcubesvk.clicksPerSecond.utils.data.callback;

/**
 * Callback for asynchronous writing operations.
 */
public abstract class WriteCallback {

    //Message delay
    private int messageDelay;

    /**
     * Initializes the messaging delay.
     *
     * @param messageDelay the messaging delay
     */
    public WriteCallback(int messageDelay) {
        this.messageDelay = messageDelay;
    }

    /**
     * Called per set delay to notify the command sender about the copying process.
     *
     * @param written amount of records written
     * @param total   total amount of records to be written
     * @param percent indicates in percent how many records have been written against the total amount
     */
    public abstract void message(long written, long total, int percent);

    /**
     * Returns the messaging delay associated with this callback messenger.
     *
     * @return the messaging delay given in constructor of this instance
     */
    public int getDelay() {
        return messageDelay;
    }
}
