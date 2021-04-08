package com.davidcubesvk.clicksPerSecond.api;

/**
 * Class used to store date and time.
 */
public class TimeHolder {

    //Date and Time
    private String date, time;

    /**
     * Initializes the holder with the specified date and time.
     *
     * @param date the date
     * @param time the time
     */
    public TimeHolder(String date, String time) {
        //Set
        this.date = date;
        this.time = time;
    }

    /**
     * Returns the date assigned to this holder.
     *
     * @return the date assigned to this holder
     */
    public String getDate() {
        return date;
    }

    /**
     * Returns the time assigned to this holder.
     *
     * @return the time assigned to this holder
     */
    public String getTime() {
        return time;
    }
}
