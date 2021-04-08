package com.davidcubesvk.clicksPerSecond.utils.data.reformatter;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.database.description.ColumnDescription;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import com.davidcubesvk.clicksPerSecond.utils.data.database.description.TableDescription;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Class covering format versions and the reformatting process.
 */
public class Reformatter {

    //Latest format version
    public static final int LATEST_FORMAT_VERSION = 25;
    //Format version
    private Map<ScoreboardType, Integer> formatVersions = new HashMap<>();
    //Scoreboards that need to be reformatted
    private Set<ScoreboardType> toReformat = new HashSet<>();

    //DecimalFormat
    private DecimalFormat decimalFormat;

    /**
     * Initializes internal variables.
     */
    public Reformatter() {
        //Get roundTo
        int roundTo = TestManager.getInstance().getCPSRoundTo();
        //Create DecimalFormat
        if (roundTo < 1) {
            //Round to int
            decimalFormat = new DecimalFormat("#");
        } else {
            //Create array
            char[] patternChars = new char[roundTo];
            //Fill
            Arrays.fill(patternChars, '#');
            //Initialize
            decimalFormat = new DecimalFormat("#." + new String(patternChars));
        }
    }

    /**
     * Refreshes the format version in the currently used data storage.
     * <p></p>
     * This method must be called asynchronously if {@link ClicksPerSecond#getStorageType()} equals <code>DATABASE</code>.
     *
     * @param announce if to announce older-format versions
     */
    public void refreshFormatVersion(boolean announce) {
        //Clear to reformat
        toReformat.clear();

        //Get format versions
        switch (ClicksPerSecond.getStorageType()) {
            case FILE:
                //Loop through every scoreboard
                for (ScoreboardType scoreboardType : ScoreboardType.values()) {
                    //Format version
                    int formatVersion;
                    //If contains the format version
                    if (ClicksPerSecond.getScoreboard().contains("formatVersion." + scoreboardType.getName()))
                        //Get from the file
                        formatVersion = ClicksPerSecond.getScoreboard().getInt("formatVersion." + scoreboardType.getName());
                    else
                        //Only version that does not contain format versions is 2.4
                        formatVersion = 24;

                    //Save and announce
                    saveAnnounceFormatVersion(scoreboardType, formatVersion, announce);
                }
                break;
            case DATABASE:
                //Loop through every scoreboard
                for (ScoreboardType scoreboardType : ScoreboardType.values()) {
                    //Get database table description
                    List<ColumnDescription> description = Database.getInstance().getDescription(scoreboardType);

                    //If null
                    if (description == null) {
                        formatVersions.put(scoreboardType, null);
                        return;
                    }

                    out:
                    for (TableDescription scoreboardsDescription : TABLE_FORMATS.keySet()) {
                        //Get table description
                        List<ColumnDescription> tableDesc = scoreboardsDescription.getDescription(scoreboardType);

                        //If not the same column amount, continue
                        if (tableDesc.size() != description.size())
                            continue;

                        //Check all
                        for (int index = 0; index < tableDesc.size(); index++) {
                            //If does not equal, continue straight to next table description
                            if (!tableDesc.get(index).equalsDesc(description.get(index)))
                                continue out;
                        }

                        //Get format version
                        int formatVersion = TABLE_FORMATS.get(scoreboardsDescription);
                        //Save and announce
                        saveAnnounceFormatVersion(scoreboardType, formatVersion, announce);
                        break;
                    }
                }
                break;
        }

        //Inform if any scoreboard is outdated and printing the message is enabled
        if (toReformat.size() > 0 && announce)
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "Scoreboards (" + toReformat.size() + ") use an outdated data format. Please use /cps reformat to reformat scoreboards displayed above to the latest format (v" + LATEST_FORMAT_VERSION + ").");
    }

    /**
     * Saves (into internal {@link Map}) the format version of a scoreboard and announces it if it's an older-version format (and announcing is enabled).
     *
     * @param scoreboardType the scoreboard's type
     * @param formatVersion  format version of the scoreboard
     * @param announce       if to announce older-format versions
     */
    private void saveAnnounceFormatVersion(ScoreboardType scoreboardType, int formatVersion, boolean announce) {
        //Put the format version into the map
        formatVersions.put(scoreboardType, formatVersion);
        //Add into to-reformat list if the format is outdated
        if (formatVersion < LATEST_FORMAT_VERSION) {
            //Needs to be reformatted
            toReformat.add(scoreboardType);
            //Print to console if enabled
            if (announce)
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "Scoreboard " + scoreboardType.getName() + " uses an outdated data format (v" + formatVersion + ").");
        }
    }

    /**
     * Reformats the given scoreboard in file format (list containing string elements) to the latest format.
     * If no reformatting is needed, returns an empty list. Please use {@link #isReformatNeeded()} to check if reformatting is needed.
     *
     * @param scoreboardType type of the scoreboard to which do the data belong
     * @param scoreboard     the scoreboard in file format
     * @return the reformatted file scoreboard
     * @throws ParseException an exception caused by invalid date or time input
     */
    public List<TestRecord> reformatFile(ScoreboardType scoreboardType, List<String> scoreboard) throws ParseException {
        //Scoreboard to be returned
        List<TestRecord> newScoreboard = new ArrayList<>();
        //Get size
        int size = scoreboard.size();

        //Check if the data format is from version 2.4
        if (formatVersions.get(scoreboardType) == 24) {
            //Loop through every element in scoreboard
            for (int index = 0; index < size; index++) {
                //Split
                String[] data = scoreboard.get(index).split(" ");
                //UUID
                UUID uuid = UUIDFactory.fromString(data[0]);
                //Format CPS
                double CPS = Double.parseDouble(decimalFormat.format(Double.parseDouble(data[1])));
                //Format milliseconds
                long time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(data[2] + " " + data[3]).getTime();

                //Add to the list
                newScoreboard.add(new TestRecord(scoreboardType, size - index, uuid, CPS, time));
            }
        }

        //Return
        return newScoreboard;
    }

    /**
     * Reformats the given scoreboard in database format (query result containing all entries) to the latest format.
     * If no reformatting is needed, returns an empty list. Please use {@link #isReformatNeeded()} to check if reformatting is needed.
     *
     * @param scoreboardType type of the scoreboard to which do the data belong
     * @param resultSet      the scoreboard in database format
     * @return the reformatted database scoreboard
     * @throws ParseException an exception thrown during this process
     */
    public List<TestRecord> reformatDatabase(ScoreboardType scoreboardType, ResultSet resultSet) throws SQLException, ParseException {
        //Scoreboard to be returned
        List<TestRecord> newScoreboard = new ArrayList<>();
        //Create date format
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //Check if the data format is from version 2.4
        if (formatVersions.get(scoreboardType) == 24) {
            //Loop through every entry
            while (resultSet.next()) {
                //Place
                int place = resultSet.getInt("place");
                //UUID
                UUID uuid = UUIDFactory.fromString(resultSet.getString("uuid"));
                //Format CPS
                double CPS = Double.parseDouble(decimalFormat.format(resultSet.getDouble("cps")));
                //Format milliseconds
                long time = simpleDateFormat.parse(resultSet.getString("d") + " " + resultSet.getString("t")).getTime();

                //Add to the list
                newScoreboard.add(new TestRecord(scoreboardType, place, uuid, CPS, time));
            }
        }

        //Return
        return newScoreboard;
    }

    /**
     * Called when the reformatting process of a scoreboard has finished to update it's format version.
     *
     * @param scoreboardType the scoreboard that has been reformatted
     */
    public void updateVersion(ScoreboardType scoreboardType) {
        //Update version
        formatVersions.put(scoreboardType, LATEST_FORMAT_VERSION);
        //Remove from to-reformat
        toReformat.remove(scoreboardType);
    }

    /**
     * Returns if any scoreboard uses an outdated data format.
     *
     * @return if any scoreboard uses an outdated data format
     */
    public boolean isReformatNeeded() {
        return toReformat.size() > 0;
    }

    /**
     * Returns if data format versions are loaded for all scoreboards (e.g. they are not <code>null</code>).
     *
     * @return if data format versions are loaded for all scoreboards
     */
    public boolean isLoaded() {
        return formatVersions.get(ScoreboardType.RIGHT) != null && formatVersions.get(ScoreboardType.LEFT) != null && formatVersions.get(ScoreboardType.HACK) != null;
    }

    /**
     * Returns if the plugin can perform any operation, i.e. if the format versions are loaded and none of them is outdated.
     *
     * @return if the plugin can perform any operation from the side of format versions
     */
    public boolean canPerformOperations() {
        return isLoaded() && !isReformatNeeded();
    }

    /**
     * Returns the data format version used by the given scoreboard.
     *
     * @param scoreboardType the scoreboard to return it's format version
     * @return the data format version used by the given scoreboard
     */
    public Integer getFormatVersion(ScoreboardType scoreboardType) {
        return formatVersions.get(scoreboardType);
    }

    /**
     * Returns all scoreboards that use an outdated data format.
     *
     * @return all scoreboards that use an outdated data format
     */
    public Set<ScoreboardType> getToReformat() {
        return toReformat;
    }

    /**
     * Returns the instance of this class.
     *
     * @return the instance of this class
     */
    public static Reformatter getInstance() {
        return ClicksPerSecond.getReformatter();
    }

    //Table formats
    private static final Map<TableDescription, Integer> TABLE_FORMATS = new HashMap<TableDescription, Integer>() {{
        //2.4
        put(new TableDescription(
                        //RIGHT
                        Arrays.asList(
                                new ColumnDescription("id", "int(11)", "NO", "PRI", null, "auto_increment"),
                                new ColumnDescription("place", "int(11)", "YES", "", null, ""),
                                new ColumnDescription("uuid", "char(32)", "NO", "PRI", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("d", "varchar(255)", "YES", "", null, ""),
                                new ColumnDescription("t", "varchar(255)", "YES", "", null, "")),
                        //LEFT
                        Arrays.asList(
                                new ColumnDescription("id", "int(11)", "NO", "PRI", null, "auto_increment"),
                                new ColumnDescription("place", "int(11)", "YES", "", null, ""),
                                new ColumnDescription("uuid", "char(32)", "NO", "PRI", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("d", "varchar(255)", "YES", "", null, ""),
                                new ColumnDescription("t", "varchar(255)", "YES", "", null, "")),
                        //HACK
                        Arrays.asList(
                                new ColumnDescription("id", "int(11)", "NO", "PRI", null, "auto_increment"),
                                new ColumnDescription("place", "int(11)", "YES", "", null, ""),
                                new ColumnDescription("uuid", "char(32)", "NO", "", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("d", "varchar(255)", "YES", "", null, ""),
                                new ColumnDescription("t", "varchar(255)", "YES", "", null, "")))
                , 24);
        //2.5
        put(new TableDescription(
                        //RIGHT
                        Arrays.asList(
                                new ColumnDescription("uuid", "char(36)", "NO", "PRI", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("t", "bigint(20) unsigned", "YES", "", null, "")),
                        //LEFT
                        Arrays.asList(
                                new ColumnDescription("uuid", "char(36)", "NO", "PRI", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("t", "bigint(20) unsigned", "YES", "", null, "")),
                        //HACK
                        Arrays.asList(
                                new ColumnDescription("id", "int(11)", "NO", "PRI", null, "auto_increment"),
                                new ColumnDescription("uuid", "char(36)", "YES", "", null, ""),
                                new ColumnDescription("cps", "double(255,30)", "YES", "", null, ""),
                                new ColumnDescription("t", "bigint(20) unsigned", "YES", "", null, "")))
                , 25);
    }};
}
