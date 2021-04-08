package com.davidcubesvk.clicksPerSecond.utils.data.database.statement;

import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;

import java.util.HashMap;
import java.util.Map;

/**
 * Class covering all statement groups.
 */
public class Statements {

    /**
     * Enum representing all actions with database done within this plugin (all possible statements used in this plugin).
     * - WAIT_TIMEOUT: query for wait_timeout variable
     * - SELECT_VOID: query for an empty result, used in connection keeper
     * - CREATE_TABLE: statement for creating scoreboard table
     * - WRITE: statement for writing data
     * - GET_ALL: statement for getting all data from the database
     * - DESCRIBE: statement for getting table's description
     * - CREATE_TABLE: statement for dropping scoreboard table
     */
    public enum Type {
        WAIT_TIMEOUT("SHOW SESSION VARIABLES LIKE 'wait_timeout'"),
        CREATE_TABLE(null),
        WRITE("INSERT INTO %(uuid, cps, t) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE cps=?, t=?"),
        GET_ALL("SELECT * FROM %"),
        DESCRIBE("DESCRIBE %"),
        DROP_TABLE("DROP TABLE %"),
        SELECT_VOID("SELECT 0 LIMIT 0");

        //Statement string
        private String statementString;

        /**
         * Initializes the constant's universal query statement for all scoreboards (if there's any, otherwise <code>null</code>).
         *
         * @param statementString the statement string
         */
        Type(String statementString) {
            this.statementString = statementString;
        }
    }

    //All statement groups
    private Map<Type, StatementGroup> statements = new HashMap<>();

    /**
     * Initializes all statement strings with the given table names.
     *
     * @param tableNames table name of each scoreboard in the database
     */
    public Statements(Map<ScoreboardType, String> tableNames) {
        //Initialize all statements
        for (Type type : Type.values()) {
            //If the string is not null
            if (type.statementString != null) {
                //Initialize for all scoreboards by replacing % with the table name if there's a scoreboard placeholder
                if (type.statementString.contains("%")) {
                    statements.put(type, new StatementGroup(
                            type.statementString.replace("%", tableNames.get(ScoreboardType.RIGHT)),
                            type.statementString.replace("%", tableNames.get(ScoreboardType.LEFT)),
                            type.statementString.replace("%", tableNames.get(ScoreboardType.HACK))
                    ));
                } else {
                    //Initialize for all scoreboards with the same statement, no changes
                    statements.put(type, new StatementGroup(type.statementString, type.statementString, type.statementString));
                }
            } else {
                //If null, it is the CREATE_TABLE, initialize separately
                statements.put(type, new StatementGroup(
                        "CREATE TABLE IF NOT EXISTS " + tableNames.get(ScoreboardType.RIGHT) + "(uuid char(36), cps double(255, 30), t BIGINT(20) UNSIGNED, PRIMARY KEY(uuid))",
                        "CREATE TABLE IF NOT EXISTS " + tableNames.get(ScoreboardType.LEFT) + "(uuid char(36), cps double(255, 30), t BIGINT(20) UNSIGNED, PRIMARY KEY(uuid))",
                        "CREATE TABLE IF NOT EXISTS " + tableNames.get(ScoreboardType.HACK) + "(id int(11) NOT NULL AUTO_INCREMENT, uuid char(36), cps double(255, 30), t BIGINT(20) UNSIGNED, PRIMARY KEY(id))"
                ));
            }
        }
    }

    /**
     * Returns the statement group of the given type containing statement strings for each scoreboard.
     *
     * @param type the type of statement group
     * @return the statement group by the given type
     */
    public StatementGroup getGroup(Type type) {
        return statements.get(type);
    }
}
