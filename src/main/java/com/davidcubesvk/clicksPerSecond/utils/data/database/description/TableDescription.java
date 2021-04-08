package com.davidcubesvk.clicksPerSecond.utils.data.database.description;

import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a description of a table.
 */
public class TableDescription {

    //Tables' descriptions
    private Map<ScoreboardType, List<ColumnDescription>> descriptions = new HashMap<>();

    /**
     * Initializes descriptions by the given database table descriptions of right, left, hack scoreboard.
     *
     * @param right the description of right scoreboard's table
     * @param left  the description of left scoreboard's table
     * @param hack  the description of hack scoreboard's table
     */
    public TableDescription(List<ColumnDescription> right, List<ColumnDescription> left, List<ColumnDescription> hack) {
        //Put into the map
        descriptions.put(ScoreboardType.RIGHT, right);
        descriptions.put(ScoreboardType.LEFT, left);
        descriptions.put(ScoreboardType.HACK, hack);
    }

    /**
     * Returns the database table description of the given scoreboard.
     *
     * @param scoreboardType the scoreboard type
     * @return the database table description
     */
    public List<ColumnDescription> getDescription(ScoreboardType scoreboardType) {
        return descriptions.get(scoreboardType);
    }
}
