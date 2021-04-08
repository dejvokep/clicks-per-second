package com.davidcubesvk.clicksPerSecond.test;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import com.davidcubesvk.clicksPerSecond.utils.task.RunnableTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Class that represents all processes and mechanics associated with a CPS test.
 */
public class Test {

    /**
     * Enum representing the result of a CPS check.
     * - NOT_PROCEED: player has gone offline or has more than allowed amount of CPS
     * - SAVE: CPS are within allowed range - save CPS
     * - NOT_SAVE: player has more than allowed amount of CPS, but has the bypass permission - do not save CPS
     */
    enum CheckCPSResult {
        NOT_PROCEED, SAVE, NOT_SAVE
    }

    /**
     * Enum representing CPS-test status.
     * - NOT_STARTED: test has not started yet
     * - RUNNING: test has started, but has not finished yet
     * - FINISHED: test has finished
     */
    enum TestStatus {
        NOT_STARTED, RUNNING, FINISHED
    }

    /**
     * Enum representing cause why the CPS-test should be ended.
     * - NORMAL: test ended after the configured duration
     * - CANCEL: test was cancelled using the cancel command
     * - DISCONNECTED: player has disconnected
     */
    public enum EndCause {
        NORMAL, CANCEL, DISCONNECTED
    }

    //Player
    private Player player;
    //Test status
    private TestStatus testStatus = TestStatus.NOT_STARTED;

    //Clicks performed in this test
    private long clicks = 0L;
    //Click mode (RIGHT or LEFT)
    private ScoreboardType clickType;
    //Location of the player before the test
    private Location locationBefore;
    //Permanent actions instance
    private PermanentAction permanentAction;
    //Duration of this test
    private long duration;

    //Ticks passed since the test start
    private long ticksPassed = 0L;
    //Pre-calculated CPS
    private double CPS = 0;

    //CPS pre-calculation delay
    private long precalculateCPS;
    //DecimalFormat for CPS rounding
    private DecimalFormat decimalFormat;

    /**
     * Initializes and starts CPS-test for the given player.
     *
     * @param player who to start the test for
     */
    Test(Player player) {
        //Set the player
        this.player = player;

        //Set the CPS pre-calculation delay
        precalculateCPS = TestManager.getInstance().getCalculateCPS();
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

        //Start the test
        start();
    }

    /**
     * Starts the CPS test for the player given in constructor.
     */
    private synchronized void start() {
        //Check the status
        if (testStatus == TestStatus.NOT_STARTED)
            //Set to running
            testStatus = TestStatus.RUNNING;
        else
            //Return if the test is running or has finished already
            return;

        //Set the last location
        locationBefore = player.getLocation();

        //Run once-actions
        TestManager.getInstance().requestAction("start.once").run(player);
        //Run permanent actions
        permanentAction = new PermanentAction(player, TestManager.getInstance().requestAction("start.permanent"));
    }

    /**
     * Checks if the click type equals the test mode (or determines the test mode if it is the first performed click) and adds a click.
     *
     * @param action the performed action type
     */
    synchronized void addClick(Action action) {
        //If not running
        if (testStatus != TestStatus.RUNNING)
            return;

        //Check if the action is right click and if it's enabled
        if (action.name().contains("RIGHT") && !TestManager.getInstance().isRightClickEnabled())
            return;

        //Set click type if null
        if (clickType == null)
            clickType = action.toString().contains("RIGHT") ? ScoreboardType.RIGHT : ScoreboardType.LEFT;

        //If action is not the same as first
        if (!action.toString().contains(clickType.name()))
            return;

        //Run one-actions
        TestManager.getInstance().requestAction("run.perClick." + clickType.getName())
                .run(player, (player, message) -> setPlaceholders(message));

        //If it is first click, copy data
        if (clicks == 0) {
            //Set duration
            duration = TestManager.getInstance().getTestDuration();

            //Pre-calculate CPS and end automatically
            runChecksEnd();

            //Stop permanent actions
            permanentAction.cancel();
            //Send new permanent actions
            permanentAction = new PermanentAction(player, TestManager.getInstance().requestAction("run.permanent." + clickType.getName()),
                    (player, message) -> setPlaceholders(message));
        }

        //Add one click to total clicks value
        clicks++;
    }

    /**
     * Ends and saves the test results (depending on CPS check).
     *
     * @param endCause reason of ending the test
     */
    synchronized void end(EndCause endCause) {
        //If finished already
        if (testStatus == TestStatus.FINISHED)
            return;

        //Set the status to finished
        testStatus = TestStatus.FINISHED;
        //Cancel titles
        permanentAction.cancel();

        //Teleport player back if enabled
        if (TestManager.getInstance().isTeleportBack())
            player.teleport(locationBefore);

        //Run actions and return if not ended normally
        switch (endCause) {
            case CANCEL:
                //Run cancel actions
                TestManager.getInstance().requestAction("end.cancel").run(player);
                return;
            case DISCONNECTED:
                return;
        }

        //Calculate and check final CPS
        CheckCPSResult checkCPSResult = checkCPS(true);
        //Do not proceed if player did not have allowed CPS
        if (checkCPSResult == CheckCPSResult.NOT_PROCEED)
            return;

        //API
        ClicksPerSecondAPI api = ClicksPerSecondAPI.getInstance();
        //Get player's best record
        TestRecord record = api.getPlayerData(player.getUniqueId(), clickType);
        //Best CPS
        Double bestCPS = record == null ? null : record.getCPS();

        //Mode
        String mode = bestCPS == null || CPS > bestCPS ? "best" : "normal";
        //Run actions
        TestManager.getInstance().requestAction("end.normal." + mode + "." + clickType.getName())
                .run(player, (player, message) -> setPlaceholders(message)
                        .replace("{cps_best_int}", "" + (bestCPS == null ? api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.CPS_INT) : (int) ((double) bestCPS)))
                        .replace("{cps_best_decimal}", "" + (bestCPS == null ? api.getPlaceholderReplacement(ClicksPerSecondAPI.RecordPlaceholder.CPS_DECIMAL) : bestCPS)));

        //Save new CPS
        if (mode.equals("best") && checkCPSResult == CheckCPSResult.SAVE)
            saveRecord(player, new TestRecord(clickType, null, player.getUniqueId(), CPS, System.currentTimeMillis()));
    }

    /**
     * Checks (calculates) the players's CPS, runs allowed.actions actions if needed and returns the result of this check represented by an enum constant.
     *
     * @param calculate indicates if calculating CPS before checking them is needed (<code>false</code> only if called by repeating task pre-calculating CPS)
     * @return the result of the CPS check
     */
    private CheckCPSResult checkCPS(boolean calculate) {
        //Calculate CPS if needed
        if (calculate)
            CPS = Double.parseDouble(decimalFormat.format(clicks / ((double) duration / 20)));

        //If player has allowed CPS, save normally
        if (CPS <= TestManager.getInstance().getAllowedCPS())
            return CheckCPSResult.SAVE;

        //If player does not have bypass permission
        if (!player.hasPermission("cps.bypass.autoClick") &&
                !player.hasPermission("cps.bypass.*") &&
                !player.hasPermission("cps.*")) {

            //Create new record and save
            saveRecord(player, new TestRecord(ScoreboardType.HACK, null, player.getUniqueId(), CPS, System.currentTimeMillis()));

            //Run actions
            TestManager.getInstance().requestAction("allowed.actions")
                    .run(player, (player, message) -> setPlaceholders(message));

            //Allowed actions ran
            return CheckCPSResult.NOT_PROCEED;
        } else {
            //Do not save results
            return CheckCPSResult.NOT_SAVE;
        }
    }

    /**
     * Pre-calculates, checks the CPS and ends the test automatically.
     */
    private void runChecksEnd() {
        //Set to 0 at the beginning
        CPS = 0;

        if (precalculateCPS > 0)
            //Run every 1tick if recalculating CPS
            new RunnableTask() {
                @Override
                public void run() {
                    //Cancel if the test has finished already
                    if (testStatus != TestStatus.RUNNING) {
                        //Cancel and return
                        cancel();
                        return;
                    }

                    //Add a tick
                    ticksPassed++;

                    //Pre-calculate CPS if enabled and the delay passed
                    if (precalculateCPS > 0 && ticksPassed % precalculateCPS == 0) {
                        //Calculate
                        CPS = Double.parseDouble(decimalFormat.format((double) clicks / ((double) ticksPassed / 20)));
                        //Check
                        checkCPS(false);
                    }

                    //End the test if after it's duration
                    if (ticksPassed >= duration)
                        TestManager.getInstance().endTest(player, EndCause.NORMAL);
                }
            }.runTimerAsync(ClicksPerSecond.getPlugin(), 1L, 1L);
        else
            //Run just to end the test after it's duration
            new RunnableTask() {
                @Override
                public void run() {
                    //Return if the test has finished already
                    if (testStatus != TestStatus.RUNNING)
                        return;

                    TestManager.getInstance().endTest(player, EndCause.NORMAL);
                }
            }.runLater(ClicksPerSecond.getPlugin(), duration);
    }

    /**
     * Replaces (sets) clicks, cps_int and cps_decimal placeholders in a message.
     *
     * @param message the message to replace placeholders in
     * @return the message with replaced placeholders
     */
    private String setPlaceholders(String message) {
        return message.replace("{clicks}", "" + clicks)
                .replace("{cps_int}", "" + (int) CPS)
                .replace("{cps_decimal}", "" + CPS);
    }

    /**
     * Saves the given record.
     *
     * @param testRecord the test record to save
     */
    private void saveRecord(Player player, TestRecord testRecord) {
        //Run async
        Bukkit.getScheduler().runTaskAsynchronously(ClicksPerSecond.getPlugin(), () -> {
            //Write
            Database.OperationResult operationResult = ClicksPerSecond.getStorageOperatorByType(ClicksPerSecond.getStorageType()).write(testRecord);

            //Send message by error if the writing process did not succeed and the player is still online
            if (player.isOnline())
                //Send message by error
                switch (operationResult) {
                    case ERR_DISCONNECT:
                        //A database error
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                ClicksPerSecond.getConfiguration().getString("test.writeError.database.disconnected")));
                        break;
                    case ERR_OTHER:
                        //Send by currently used data storage
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                ClicksPerSecond.getConfiguration().getString("test.writeError." +
                                        (ClicksPerSecond.getStorageType() == StorageType.DATABASE ? "database.other" : "file.error"))));
                        break;
                }
        });
    }

    /**
     * Returns the test's status.
     *
     * @return the test's status
     */
    public TestStatus getStatus() {
        return testStatus;
    }
}