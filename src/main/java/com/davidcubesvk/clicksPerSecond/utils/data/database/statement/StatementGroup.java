package com.davidcubesvk.clicksPerSecond.utils.data.database.statement;

import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;

import java.util.HashMap;
import java.util.Map;

/**
 * Class covering one statement group.
 */
public class StatementGroup {

    //Statements for all scoreboards
    private Map<ScoreboardType, String> statements = new HashMap<>();

    /**
     * Initializes this group by the given statement string for each scoreboard.
     *
     * @param right statement for <code>RIGHT</code> scoreboard
     * @param left  statement for <code>LEFT</code> scoreboard
     * @param hack  statement for <code>HACK</code> scoreboard
     */
    StatementGroup(String right, String left, String hack) {
        //Initialize
        statements.put(ScoreboardType.RIGHT, right);
        statements.put(ScoreboardType.LEFT, left);
        statements.put(ScoreboardType.HACK, hack);
    }

    /**
     * Returns a statement string for the given scoreboard.
     *
     * @param scoreboardType the target scoreboard of the returned statement string
     * @return the statement string to be executed towards the given scoreboard
     */
    public String getStatement(ScoreboardType scoreboardType) {
        return statements.get(scoreboardType);
    }
}
