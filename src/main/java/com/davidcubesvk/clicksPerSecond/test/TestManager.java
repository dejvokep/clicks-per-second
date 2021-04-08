package com.davidcubesvk.clicksPerSecond.test;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CPS-test manager.
 */
public class TestManager {

    //All actions
    private Map<String, com.davidcubesvk.clicksPerSecond.utils.action.Action> actions = new HashMap<>();
    //Booleans
    private boolean teleportBack, rightClickEnabled;
    //CPS test duration, calculate CPS delay
    private long duration, calculateCPS;
    //Maximum allowed CPS
    private double allowedCPS;
    //RoundTo variable
    private int roundTo;

    //All running tests
    private Map<UUID, Test> active = new HashMap<>();
    //Action paths
    public static final String[] ACTION_PATHS = {"allowed.actions", "start.once", "start.permanent",
            "run.perClick.right", "run.perClick.left", "run.permanent.right", "run.permanent.left",
            "end.normal.normal.right", "end.normal.normal.left", "end.normal.best.right", "end.normal.best.left", "end.cancel"};

    /**
     * Loads internal data.
     */
    public TestManager() {
        reload();
    }

    /**
     * Starts a CPS-test for the given player.
     *
     * @param player the player to start the test for
     */
    public void startTest(Player player) {
        //Check if running
        if (isInTest(player.getUniqueId()))
            return;

        //Create new test
        active.put(player.getUniqueId(), new Test(player));
    }

    /**
     * Ends the CPS-test (owned by the given player) with the specified cause.
     *
     * @param player   the owner of the CPS-test
     * @param endCause the test end cause
     */
    public void endTest(Player player, Test.EndCause endCause) {
        //Get test
        Test test = getTest(player.getUniqueId());

        //If isn't in test or the test finished already
        if (test == null || test.getStatus() == Test.TestStatus.FINISHED)
            return;

        //End the test
        test.end(endCause);
        //Remove as active
        active.remove(player.getUniqueId());
    }

    /**
     * Adds a click in a CPS-test owned by the given UUID (represents a player).
     *
     * @param uuid   the UUID of player who clicked
     * @param action performed action type
     */
    public void addClick(UUID uuid, Action action) {
        //Get player's CPS-test
        Test test = getTest(uuid);

        //If is in test
        if (test != null)
            test.addClick(action);
    }

    /**
     * Reloads internal data.
     */
    public void reload() {
        //Get config
        Configuration config = ClicksPerSecond.getConfiguration();

        //Test variables
        duration = config.getLong("test.duration");
        calculateCPS = config.getLong("test.calculateCPS");
        allowedCPS = config.getDouble("test.allowed.cps");
        rightClickEnabled = config.getBoolean("test.enableRightClick");

        //Teleport back
        teleportBack = config.getBoolean("end.teleportBack");
        //Get how many digits to round cps_decimal to
        roundTo = config.getInt("roundTo");

        //Reset the map full of actions
        actions.clear();
        //Add by path
        for (String path : ACTION_PATHS)
            actions.put(path, new com.davidcubesvk.clicksPerSecond.utils.action.Action(config.getStringList("test." + path)));
    }

    /**
     * Returns a action object by the specified path in config.yml (without "test."), if not found, returns <code>null</code>.
     *
     * @param path path to the action in config.yml
     * @return the action corresponding to the specified path, or <code>null</code> if not found
     * @see TestManager#ACTION_PATHS
     */
    public com.davidcubesvk.clicksPerSecond.utils.action.Action requestAction(String path) {
        return actions.getOrDefault(path, null);
    }

    /**
     * Returns the CPS-test instance if player (represented by an UUID) has active any, or <code>null</code> if not found.
     *
     * @param uuid the UUID representing a player
     * @return the test instance or <code>null</code> if not found
     */
    public Test getTest(UUID uuid) {
        return active.getOrDefault(uuid, null);
    }

    /**
     * Returns if the given UUID (representing a player) has an active CPS-test or not.
     *
     * @param uuid the UUID representing player
     * @return if the player has any active CPS-test
     */
    public boolean isInTest(UUID uuid) {
        return active.containsKey(uuid);
    }

    /**
     * Returns if the teleporting back after the CPS-test has finished is enabled.
     *
     * @return if the <code>teleportBack</code> option is enabled
     */
    public boolean isTeleportBack() {
        return teleportBack;
    }

    /**
     * Returns if right-click mode is enabled.
     *
     * @return if right-click mode is enabled
     */
    public boolean isRightClickEnabled() {
        return rightClickEnabled;
    }

    /**
     * Returns the CPS-test duration as configured in config.yml.
     *
     * @return the CPS-test duration
     */
    public long getTestDuration() {
        return duration;
    }

    /**
     * Returns the delay between each CPS pre-calculation (and CPS-check) during the CPS-test.
     *
     * @return the delay between each CPS pre-calculation (and CPS-check)
     */
    public long getCalculateCPS() {
        return calculateCPS;
    }

    /**
     * Returns the maximum allowed CPS as configured in config.yml.
     *
     * @return the maximum allowed CPS
     */
    public double getAllowedCPS() {
        return allowedCPS;
    }

    /**
     * Returns to how many digits CPS should be rounded as configured in config.yml.
     *
     * @return to how many digits CPS should be rounded
     */
    public int getCPSRoundTo() {
        return roundTo;
    }

    /**
     * Returns the instance of this class.
     *
     * @return the instance of this class
     */
    public static TestManager getInstance() {
        return ClicksPerSecond.getTestManager();
    }
}
