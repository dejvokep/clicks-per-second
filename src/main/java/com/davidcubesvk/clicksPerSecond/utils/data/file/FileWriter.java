package com.davidcubesvk.clicksPerSecond.utils.data.file;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.async.ObjectInt;
import com.davidcubesvk.clicksPerSecond.utils.data.DataGetResult;
import com.davidcubesvk.clicksPerSecond.utils.data.DataStorageOperator;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.WriteCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.ReformatCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.UUIDFactory;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import com.davidcubesvk.clicksPerSecond.utils.task.RunnableTask;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * Scoreboard file operator.
 */
public class FileWriter implements DataStorageOperator {

    //If reformatting process is active
    private boolean reformatActive = false;

    /**
     * Calls {@link Reformatter#refreshFormatVersion(boolean)} to refresh the data format version (if {@link ClicksPerSecond#getStorageType()} equals <code>FILE</code>).
     */
    public FileWriter() {
        //Refresh the data format if using this storage
        if (ClicksPerSecond.getStorageType() == StorageType.FILE)
            Reformatter.getInstance().refreshFormatVersion(true);
    }

    @Override
    public synchronized Database.OperationResult write(TestRecord testRecord) {
        return writeAll(new LinkedList<>(Collections.singletonList(testRecord)), null);
    }

    @Override
    public synchronized Database.OperationResult writeAll(Queue<TestRecord> testRecords) {
        return writeAll(testRecords, null);
    }

    @Override
    public synchronized Database.OperationResult writeAll(Queue<TestRecord> testRecords, WriteCallback writeCallback) {
        //Format checks
        if (!Reformatter.getInstance().isLoaded() || Reformatter.getInstance().isReformatNeeded() || reformatActive)
            return Database.OperationResult.ERR_REFORMAT;
        //If the given queue is empty
        if (testRecords.size() == 0)
            return Database.OperationResult.SUCCESS;

        //Get the scoreboard type from the first element
        ScoreboardType scoreboardType = new ArrayList<>(testRecords).get(0).getScoreboardType();
        //File instance
        FileConfiguration scoreboard = ClicksPerSecond.getScoreboard();
        //Scoreboard name
        String scoreboardName = scoreboardType.getName();

        //Current scoreboard from the list
        List<String> scoreboardList = new ArrayList<>();
        //Get from the file if exists
        if (scoreboard.contains(scoreboardName))
            scoreboardList = scoreboard.getStringList(scoreboardName);

        //Written records
        ObjectInt written = new ObjectInt(0);
        //Repeating task
        RunnableTask repeating = Database.repeatingWriteMessenger(writeCallback, written, testRecords.size());
        //Record that is being written currently (for re-inserting in case of an exception)
        TestRecord current = null;

        try {
            //Adding records to the scoreboard
            if (scoreboardType == ScoreboardType.RIGHT || scoreboardType == ScoreboardType.LEFT) {
                //Loop through all
                while (testRecords.size() > 0) {
                    //Get the current one
                    current = testRecords.poll();
                    //Get the index of the UUID in scoreboard
                    int index = indexOf(scoreboardList, current.getUuid());

                    //If found, remove
                    if (index != -1)
                        scoreboardList.remove(index);

                    //Add to the list
                    scoreboardList.add(toString(current, 0));

                    //Increase
                    written.change(1);
                }
            } else {
                //Loop through all
                while (testRecords.size() > 0) {
                    //Get the current one
                    current = testRecords.poll();
                    //Add to the list with the ID
                    scoreboardList.add(0, toString(current, scoreboardList.size()));

                    //Increase
                    written.change(1);
                }
            }
        } catch (Exception ex) {
            //Add the last record back
            if (current != null)
                testRecords.add(current);
            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while writing to the scoreboard file (ERR_OTHER)!", ex);
            //Return
            return Database.OperationResult.ERR_OTHER;
        }

        //Cancel the task if not null
        if (repeating != null && !repeating.isCancelled())
            repeating.cancel();

        //Update the file
        scoreboard.set(scoreboardName, scoreboardList);
        //Save
        ClicksPerSecond.saveScoreboard();

        //Send 100% message
        if (writeCallback != null)
            writeCallback.message(written.get(), written.get(), 100);

        return Database.OperationResult.SUCCESS;
    }

    @Override
    public DataGetResult getAllData(ScoreboardType scoreboardType) {
        //Create a list to return
        List<TestRecord> scoreboard = new ArrayList<>();

        //Format checks
        if (!Reformatter.getInstance().isLoaded() || Reformatter.getInstance().isReformatNeeded() || reformatActive)
            return new DataGetResult(Database.OperationResult.ERR_REFORMAT, null);
        //Return if scoreboard does not exist in the file
        if (!ClicksPerSecond.getScoreboard().contains(scoreboardType.getName()))
            return new DataGetResult(Database.OperationResult.SUCCESS, scoreboard);

        try {
            //Get scoreboard from the file
            List<String> rawScoreboard = ClicksPerSecond.getScoreboard().getStringList(scoreboardType.getName());

            //Loop through the whole scoreboard
            for (String dataLine : rawScoreboard) {
                //Get data by splitting
                String[] data = dataLine.split(" ");

                //Create and add the TestRecord to the list
                scoreboard.add(scoreboardType == ScoreboardType.HACK ?
                        new TestRecord(scoreboardType, Integer.valueOf(data[0]), UUIDFactory.fromString(data[1]), Double.parseDouble(data[2]), Long.parseLong(data[3])) :
                        new TestRecord(scoreboardType, null, UUIDFactory.fromString(data[0]), Double.parseDouble(data[1]), Long.parseLong(data[2])));
            }

            //Order by CPS or ID (depending on scoreboard type, TestRecord's internal)
            scoreboard.sort(Comparator.reverseOrder());

            //Set places in scoreboard if not HACK scoreboard
            if (scoreboardType != ScoreboardType.HACK)
                //Set places
                for (int index = 0; index < scoreboard.size(); index++)
                    scoreboard.get(index).setPlace(index + 1);
        } catch (Exception ex) {
            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while getting all data from the scoreboard file (ERR_OTHER)!", ex);
            //Return
            return new DataGetResult(Database.OperationResult.ERR_OTHER, null);
        }


        //Return
        return new DataGetResult(Database.OperationResult.SUCCESS, scoreboard);
    }

    @Override
    public synchronized void reformat(ReformatCallback reformatCallback) {
        //Create task field
        RunnableTask repeating = null;

        try {
            //Get reformatter
            Reformatter reformatter = Reformatter.getInstance();
            //If reformat is not needed
            if (!reformatter.isReformatNeeded())
                return;

            //Process is active
            reformatActive = true;

            //Loop through scoreboards that need to be reformatted
            for (ScoreboardType scoreboardType : new HashSet<>(reformatter.getToReformat())) {
                //Getting data
                reformatCallback.PHASE_getting(scoreboardType);
                //Get scoreboard
                List<String> scoreboard = ClicksPerSecond.getScoreboard().getStringList(scoreboardType.getName());

                //Reformatting data
                reformatCallback.PHASE_reformatting(scoreboardType);
                //Reformat
                List<TestRecord> formatted = reformatter.reformatFile(scoreboardType, scoreboard);
                int size = formatted.size();

                //If empty scoreboard
                if (size == 0) {
                    //Skipping this scoreboard
                    reformatCallback.PHASE_skipping(scoreboardType);
                    //Update the version
                    reformatter.updateVersion(scoreboardType);
                    ClicksPerSecond.getScoreboard().set("formatVersion." + scoreboardType.getName(), Reformatter.LATEST_FORMAT_VERSION);
                    ClicksPerSecond.saveScoreboard();
                    //Continue
                    continue;
                }

                //Converted records
                final ObjectInt converted = new ObjectInt(0);
                //Schedule status messenger
                repeating = new RunnableTask() {
                    @Override
                    public void run() {
                        //Converting
                        reformatCallback.PHASE_FILE_converting(scoreboardType, converted.get(), size, ((int) ((double) converted.get() / size) * 100));

                        //Send only once if repeating disabled, cancel now
                        if (reformatCallback.getMessageDelay() == -1 && !isCancelled())
                            cancel();
                    }
                }.runTimer(ClicksPerSecond.getPlugin(), 0L, reformatCallback.getMessageDelay());

                //Scoreboard in string version
                List<String> scoreboardList = new ArrayList<>();
                //Write all
                for (TestRecord testRecord : formatted) {
                    //Add the converted string
                    scoreboardList.add(toString(testRecord, 0));
                    //Increase by 1
                    converted.change(1);
                }
                //Stop the repeating task
                if (!repeating.isCancelled())
                    repeating.cancel();
                //Send 100% message
                reformatCallback.PHASE_FILE_converting(scoreboardType, size, size, 100);

                //Writing
                reformatCallback.PHASE_FILE_writing(scoreboardType);
                //Set the new scoreboard
                ClicksPerSecond.getScoreboard().set(scoreboardType.getName(), scoreboardList);
                //Save
                ClicksPerSecond.saveScoreboard();

                //Update the version
                reformatter.updateVersion(scoreboardType);
                ClicksPerSecond.getScoreboard().set("formatVersion." + scoreboardType.getName(), Reformatter.LATEST_FORMAT_VERSION);
                ClicksPerSecond.saveScoreboard();

                //Finished
                reformatCallback.PHASE_finished(scoreboardType);
            }

            //Finished
            reformatCallback.PHASE_finished();
        } catch (Exception ex) {
            //Log the error
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred during the reformatting process (ERR_OTHER)!", ex);
            reformatCallback.ERROR_FILE_error();
        }

        //Stop the repeating task if not cancelled
        if (repeating != null && !repeating.isCancelled())
            repeating.cancel();
        //Process is not active
        reformatActive = false;
    }

    @Override
    public boolean isReformatActive() {
        return reformatActive;
    }

    /**
     * Returns the first occurrence of the specified UUID in the scoreboard (represented by an index).
     * If not found, returns <code>-1</code>.
     *
     * @param scoreboard a scoreboard to search in
     * @param uuid       the UUID to find
     * @return the first occurrence index of the UUID, or <code>-1</code> if not found
     */
    private int indexOf(List<String> scoreboard, UUID uuid) {
        //Search for the UUID
        for (int index = 0; index < scoreboard.size(); index++) {
            //Return if contains
            if (scoreboard.get(index).contains(uuid.toString()))
                return index;
        }

        //Return -1, not found
        return -1;
    }

    /**
     * Converts a test record into a file-friendly string.
     *
     * @param testRecord     the test record to convert
     * @param scoreboardSize the scoreboard size (this is only used in case the record belongs to the hacking scoreboard and {@link TestRecord#getPlace()} is <code>null</code>)
     * @return the converted test record into a string
     */
    private String toString(TestRecord testRecord, int scoreboardSize) {
        return testRecord.getScoreboardType() == ScoreboardType.HACK ?
                (testRecord.getPlace() == null ? scoreboardSize + 1 : testRecord.getPlace()) + " " + testRecord.getUuid().toString() + " " + testRecord.getCPS() + " " + testRecord.getMillis() :
                testRecord.getUuid().toString() + " " + testRecord.getCPS() + " " + testRecord.getMillis();
    }

    /**
     * Returns the class instance.
     *
     * @return the class instance
     */
    public static FileWriter getInstance() {
        return ClicksPerSecond.getFileWriter();
    }

}
