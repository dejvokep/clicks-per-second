package com.davidcubesvk.clicksPerSecond.test;

import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.api.TimeHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a result of a CPS test.
 */
public class TestRecord implements Comparable<TestRecord> {

    //Record data
    private ScoreboardType scoreboardType;
    private Integer place;
    private UUID uuid;
    private double CPS;
    private long millis;
    //TimeHolder instance
    private TimeHolder timeHolder;

    //Player's name - can be null
    private String name;

    /**
     * Initializes the test record with the given data.
     *
     * @param scoreboardType scoreboard type to which does this record belong
     * @param place          place of this record in scoreboard, or <code>null</code> if unknown (if the record was created in saving process)
     * @param uuid           UUID representing player who owns this record
     * @param CPS            achieved CPS
     * @param millis         UNIX time representing time at which was this record created
     */
    public TestRecord(ScoreboardType scoreboardType, Integer place, UUID uuid, double CPS, long millis) {
        //Set values
        this.scoreboardType = scoreboardType;
        this.place = place;
        this.uuid = uuid;
        this.CPS = CPS;
        this.millis = millis;

        //Get API
        ClicksPerSecondAPI api = ClicksPerSecondAPI.getInstance();
        //Create TimeHolder
        this.timeHolder = api.formatDate(api.getDateFormat(), api.getTimeFormat(), millis);

        //Get the player
        OfflinePlayer offlinePlayer = (offlinePlayer = Bukkit.getPlayer(uuid)) == null ? Bukkit.getOfflinePlayer(uuid) : offlinePlayer;
        //Set the name if is offline or has played before
        if (offlinePlayer instanceof Player || offlinePlayer.hasPlayedBefore())
            name = offlinePlayer.getName();
    }

    @Override
    public int compareTo(TestRecord o) {
        //Compare different things for different scoreboard type
        if (scoreboardType == ScoreboardType.HACK)
            //Compare by the given ID (place)
            return Integer.compare(this.place, o.place);
        else
            //Compare by CPS
            return Double.compare(this.CPS, o.CPS);
    }

    /**
     * Replaces (sets) all record placeholders in a message.
     *
     * @param message the message to replace placeholders in
     * @return the message with replaced placeholders
     * @see #setPlaceholders(String, String)
     */
    public String setPlaceholders(String message) {
        //Use similar method
        return setPlaceholders(message, "");
    }

    /**
     * Replaces (sets) all record placeholders (with the given prefix) in a message.
     * Prefix is used as following: <code>{ + prefix + original placeholder + }</code>
     *
     * @param message the message to replace placeholders in
     * @param prefix  the prefix for placeholders
     * @return the message with replaced placeholders
     */
    public String setPlaceholders(String message, String prefix) {
        //Get API for replacements
        ClicksPerSecondAPI api = ClicksPerSecondAPI.getInstance();

        return message.replace("{" + prefix + "place}", place != null ? "" + place : api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLACE))
                .replace("{" + prefix + "player_uuid}", uuid != null ? uuid.toString() : api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLAYER_UUID))
                .replace("{" + prefix + "player_name}", name != null ? name : api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.PLAYER_NAME))
                .replace("{" + prefix + "cps_int}", "" + (int) CPS)
                .replace("{" + prefix + "cps_decimal}", "" + CPS)
                .replace("{" + prefix + "date}", timeHolder.getDate())
                .replace("{" + prefix + "time}", timeHolder.getTime());
    }

    /**
     * Sets a new place in scoreboard.
     *
     * @param place the new place in scoreboard
     */
    public void setPlace(Integer place) {
        this.place = place;
    }

    /**
     * Returns the property assigned to this record by it's type.
     *
     * @param propertyType the property type
     * @return the property assigned to this record by it's type
     */
    public Object getProperty(ClicksPerSecondAPI.RecordPlaceholder propertyType) {
        //Return by the property type
        switch (propertyType) {
            case PLACE:
                return place;
            case PLAYER_UUID:
                return uuid;
            case PLAYER_NAME:
                return name;
            case CPS_INT:
                return (int) CPS;
            case CPS_DECIMAL:
                return CPS;
            case DATE:
                return timeHolder.getDate();
            case TIME:
                return timeHolder.getTime();
        }

        return null;
    }

    /**
     * Returns the scoreboard type assigned to this record.
     *
     * @return the scoreboard type assigned to this record
     */
    public ScoreboardType getScoreboardType() {
        return scoreboardType;
    }

    /**
     * Returns the place in scoreboard assigned to this record.
     *
     * @return the place in scoreboard assigned to this record
     */
    public Integer getPlace() {
        return place;
    }

    /**
     * Returns the UUID assigned to this record.
     *
     * @return the UUID assigned to this record
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Returns the CPS assigned to this record.
     *
     * @return the CPS assigned to this record
     */
    public double getCPS() {
        return CPS;
    }

    /**
     * Returns the UNIX time at which was this record created.
     *
     * @return the UNIX time at which was this record created
     */
    public long getMillis() {
        return millis;
    }

    /**
     * Returns a time holder instance with formatted UNIX time (time at which was this record created) to date and time.
     *
     * @return the time holder instance with formatted UNIX time
     * @see #getMillis()
     */
    public TimeHolder getTimeHolder() {
        return timeHolder;
    }

    /**
     * Returns the player name corresponding to the UUID assigned to this record using {@link Bukkit#getOfflinePlayer(UUID)}.
     * If the player hasn't played on this server before, returns <code>null</code> as the name can't be obtained.
     *
     * @return the player name corresponding to the UUID assigned to this record or <code>null</code> if can't be obtained
     * @see Bukkit#getOfflinePlayer(UUID)
     */
    public String getName() {
        return name;
    }
}
