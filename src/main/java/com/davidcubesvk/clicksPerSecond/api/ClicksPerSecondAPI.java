package com.davidcubesvk.clicksPerSecond.api;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.DataGetResult;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * ClicksPerSeconds's API class.
 */
public class ClicksPerSecondAPI {

    /**
     * Enum representing all types of test record placeholders.
     * - PLACE: place placeholder
     * - PLAYER_UUID: player_uuid placeholder
     * - PLAYER_NAME: player_name placeholder
     * - CPS_INT: cps_int placeholder
     * - CPS_DECIMAL: cps_decimal placeholder
     * - DATE: date placeholder
     * - TIME: time placeholder
     */
    public enum RecordPlaceholder {
        PLACE, PLAYER_UUID, PLAYER_NAME, CPS_INT, CPS_DECIMAL, DATE, TIME;

        /**
         * Returns the key (path) to the replacement in the config.yml.
         *
         * @return the key to replacement in the config.yml
         */
        public String getConfigKey() {
            return name().toLowerCase();
        }
    }

    //All records
    private Map<ScoreboardType, List<TestRecord>> records = new HashMap<>();

    //Date and time format
    private String dateFormat, timeFormat;
    //Placeholder replacements
    private Map<RecordPlaceholder, String> placeholderReplacements = new HashMap<>();

    /**
     * Initializes the automatic scoreboard refreshing and loads internal data.
     */
    public ClicksPerSecondAPI() {
        //Reload
        reload();

        //Schedule with fixed delay
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
                //Refresh all scoreboards
                for (ScoreboardType scoreboardType : ScoreboardType.values())
                    //Refresh
                    records.put(scoreboardType, getScoreboard(scoreboardType));
        }, 0L, ClicksPerSecond.getConfiguration().getInt("refresh"), TimeUnit.MILLISECONDS);
    }

    /**
     * Reloads the config-dependent values used in this class.
     */
    public synchronized void reload() {
        //Get config
        FileConfiguration config = ClicksPerSecond.getConfiguration();

        //Set date and time format
        dateFormat = config.getString("format.date");
        timeFormat = config.getString("format.time");
        //Set all placeholder replacements
        for (RecordPlaceholder type : RecordPlaceholder.values())
            placeholderReplacements.put(type, config.getString("placeholderReplacement." + type.getConfigKey()));
    }

    /**
     * Returns the test manager instance.
     *
     * @return the test manager
     */
    public TestManager getTestManager() {
        return TestManager.getInstance();
    }

    /**
     * Returns the full scoreboard data in a list containing test records, from the file or the database (depends on the current {@link ClicksPerSecond#getStorageType()}).
     * If the scoreboard does not have any data or any error occurred during the operation, method returns an empty list.
     * <p></p>
     * This method must be called asynchronously. This method should not be called if it's not needed, use {@link #getCachedScoreboard(ScoreboardType)} instead.
     *
     * @param scoreboardType the type of the scoreboard to get
     * @return the full scoreboard in a list
     */
    public List<TestRecord> getScoreboard(ScoreboardType scoreboardType) {
        //Get storage type
        StorageType storageType = ClicksPerSecond.getStorageType();

        //If using database and it's null
        if (storageType == StorageType.DATABASE && ClicksPerSecond.getDatabase() == null)
            return new ArrayList<>();

        //Get data
        DataGetResult dataGetResult = ClicksPerSecond.getStorageOperatorByType(storageType).getAllData(scoreboardType);

        //Return the scoreboard if the operation was successful, or an empty list
        if (dataGetResult.getResult() == Database.OperationResult.SUCCESS)
            return dataGetResult.getData();
        else
            return new ArrayList<>();
    }

    /**
     * Formats the given UNIX time (milliseconds) into the specified date and time format and returns it in a time holder instance.
     * <p></p>
     * If an error occurs during the operation, returns a time holder instance with date and time set to <code>null</code>.
     *
     * @param dateFormat the date format (e.g. "yyyy-MM-dd")
     * @param timeFormat the time format (e.g. "HH:mm:ss")
     * @param millis     the UNIX time (milliseconds) to be formatted
     * @return the formatted date and time in a time holder
     */
    public TimeHolder formatDate(String dateFormat, String timeFormat, long millis) {
        try {
            //Format the date
            String date = new SimpleDateFormat(dateFormat).format(new Date(millis));
            //Get the millis of the date
            long dateMillis = new SimpleDateFormat(dateFormat).parse(date).getTime();

            //Format the time
            String time = new SimpleDateFormat(timeFormat).format(new Date(millis - dateMillis));
            //Return
            return new TimeHolder(date, time);
        } catch (ParseException ex) {
            //Error
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "Error occurred while formatting date and time!", ex);
            //Return empty holder
            return new TimeHolder(null, null);
        }
    }

    /**
     * Returns the test record owned by the given UUID (representing player) in a scoreboard.
     * <p></p>
     * If no data found, returns <code>null</code>.
     *
     * @param uuid           the UUID representing player
     * @param scoreboardType the scoreboard type
     * @return the test record instance, or <code>null</code> if not found
     */
    public TestRecord getPlayerData(UUID uuid, ScoreboardType scoreboardType) {
        //Get from the cached scoreboard
        for (TestRecord testRecord : records.get(scoreboardType))
            //If the UUIDs equal
            if (testRecord.getUuid().equals(uuid))
                return testRecord;

        //Not found
        return null;
    }

    /**
     * Returns the test record that is on the given place in a scoreboard.
     * <p></p>
     * If no data found, returns <code>null</code>.
     *
     * @param place          the place in scoreboard (indexing begins from 1)
     * @param scoreboardType the scoreboard type
     * @return the test record instance, or <code>null</code> if the place is not occupied
     */
    public TestRecord getPlaceData(int place, ScoreboardType scoreboardType) {
        //Get the scoreboard
        List<TestRecord> scoreboard = records.get(scoreboardType);

        //If out of range
        if (place < 1 || place > scoreboard.size())
            return null;

        //Return the record
        return scoreboard.get(place - 1);
    }

    /**
     * Returns the full cached scoreboard data in a list containing test records. Plugin caches scoreboards to improve performance and refreshes them per set delay in config.yml.
     * <p></p>
     * If the scoreboard does not have any data, method returns an empty list.
     *
     * @param scoreboardType the scoreboard type
     * @return the full cached scoreboard in a list
     * @see #getScoreboard(ScoreboardType)
     */
    public List<TestRecord> getCachedScoreboard(ScoreboardType scoreboardType) {
        return records.get(scoreboardType);
    }

    /**
     * Returns the placeholder replacement for a placeholder of the given type.
     *
     * @param type the placeholder type
     * @return the placeholder replacement for the given placeholder type
     */
    public String getPlaceholderReplacement(RecordPlaceholder type) {
        return placeholderReplacements.getOrDefault(type, null);
    }

    /**
     * Returns the date format used by the plugin (cached from config.yml).
     *
     * @return the date format
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Returns the time format used by the plugin (cached from config.yml).
     *
     * @return the time format
     */
    public String getTimeFormat() {
        return timeFormat;
    }

    /**
     * Returns the class instance.
     *
     * @return the class instance
     */
    public static ClicksPerSecondAPI getInstance() {
        return ClicksPerSecond.getClicksPerSecondAPI();
    }
}
