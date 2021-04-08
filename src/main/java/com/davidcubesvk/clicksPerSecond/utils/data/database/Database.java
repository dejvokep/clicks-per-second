package com.davidcubesvk.clicksPerSecond.utils.data.database;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.async.ObjectInt;
import com.davidcubesvk.clicksPerSecond.utils.data.DataGetResult;
import com.davidcubesvk.clicksPerSecond.utils.data.DataStorageOperator;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.ReformatCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.WriteCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.UUIDFactory;
import com.davidcubesvk.clicksPerSecond.utils.data.database.description.ColumnDescription;
import com.davidcubesvk.clicksPerSecond.utils.data.database.statement.Statements;
import com.davidcubesvk.clicksPerSecond.utils.task.RunnableTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Database operator.
 */
public class Database implements DataStorageOperator {

    /**
     * Enum representing all results that can be produced when writing/reading into/from the database.
     * SUCCESS - operation successful
     * ERR_DISCONNECT - connection has been lost (if writing, queued to write again when reconnected)
     * ERR_REFORMAT - reformatting process is active, data need to be reformatted, or format versions are being loaded
     * ERR_OTHER - other error has occurred, not queued
     */
    public enum OperationResult {
        SUCCESS, ERR_DISCONNECT, ERR_REFORMAT, ERR_OTHER
    }

    //Driver
    public static final String DRIVER = "com.mysql.jdbc.Driver";

    //Connection instance
    private Connection connection;
    //Connection data
    private String host, database, username, password, sslMode;
    private int port;
    //Timeouts
    private int reconnectTimeout, connectTimeout, socketTimeout;

    //Table names to use
    private Map<ScoreboardType, String> tables = new HashMap<>();
    //Connection keeper instance
    private ConnectionKeeper connectionKeeper;

    //If connected
    private boolean connected = false;
    //If the reformat process is active (shared with the post executor)
    boolean reformatActive = false;

    //Queued TestRecords
    private Queue<TestRecord> queued = new ConcurrentLinkedQueue<>(), reformatQueue = new ConcurrentLinkedQueue<>();
    //Failed reformatting process executor
    private ReformatFailPostExecutor reformatFailPostExecutor = null;
    //Statement strings
    private Statements statements;

    /**
     * Initializes internal connection data and checks for the driver.
     */
    public Database() {
        //Reload
        reload();

        //Check if the driver is installed
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException ex) {
            //Driver not installed
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "ConnectorJ not found! Disabling plugin...");
            //Disable the plugin
            Bukkit.getPluginManager().disablePlugin(ClicksPerSecond.getPlugin());
        }
    }

    /**
     * Connects to the database.
     * This method must be called asynchronously.
     */
    public synchronized void connect() {
        try {
            //If connected already
            if (isConnected())
                return;

            //Info
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Connecting to the database server on " + host + ":" + port + "...");

            //Construct url
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?sslMode=" + sslMode + "&connectTimeout=" + connectTimeout + "&socketTimeout=" + socketTimeout;
            //Connect
            connection = DriverManager.getConnection(url, username, password);
            //Set connected
            connected = true;

            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Connected to the database server (SUCCESS). Using database " + database + ".");
        } catch (SQLException ex) {
            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while connecting to the database server! Trying to connect to the database server again in " + reconnectTimeout + "s.", ex);
            //Reconnect
            Bukkit.getScheduler().runTaskLaterAsynchronously(ClicksPerSecond.getPlugin(), this::connect, reconnectTimeout * 20L);
            return;
        }

        //Run reformat post executor if there's any
        if (reformatFailPostExecutor != null)
            reformatFailPostExecutor.execute();
        //Create tables
        if (!createTables())
            return;
        //Keep the connection
        keepConnection();
        //Refresh format version
        Reformatter.getInstance().refreshFormatVersion(true);
        //Write queued records
        writeQueued();
    }

    /**
     * Disconnects from the database.
     * This method must be called asynchronously.
     */
    public synchronized void disconnect() {
        //Stop the connection keeper
        if (connectionKeeper != null) {
            //Cancel
            connectionKeeper.cancel();
            //Set to null
            connectionKeeper = null;
        }

        try {
            //If not connected
            if (!isConnected())
                return;

            //Info
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Disconnecting from the database server...");

            //Close connection
            connection.close();
            //Set disconnected
            connected = false;
            //Set connection to null
            connection = null;

            //Info
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Disconnected from the database server (SUCCESS).");
        } catch (SQLException ex) {
            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while disconnecting from the database server!", ex);
        }
    }

    /**
     * Writes all queued records if format versions are the latest.
     * If the operation result (output of used method {@link #writeAll(Queue, WriteCallback)}) is <code>ERR_OTHER</code>, calls itself again in 5 seconds.
     */
    private void writeQueued() {
        //If can't perform operations
        if (!Reformatter.getInstance().canPerformOperations())
            return;

        //Write queued records
        if (queued.size() > 0) {
            //Create callback
            WriteCallback writeCallback = new WriteCallback(20) {
                @Override
                public void message(long written, long total, int percent) {
                    ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Writing queued records... (" + written + "/" + total + " - " + percent + "%)");
                }
            };

            //Write all and run by the result
            switch (writeAll(queued, writeCallback)) {
                case ERR_DISCONNECT:
                    ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while writing queued records (ERR_DISCONNECT)! Trying again when the plugin reconnects (DO NOT SHUT DOWN THE SYSTEM).");
                    break;
                case ERR_OTHER:
                    ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while writing queued records (ERR_OTHER)! Trying again in 5 seconds (DO NOT SHUT DOWN THE SYSTEM).");
                    //Run again
                    Bukkit.getScheduler().runTaskLaterAsynchronously(ClicksPerSecond.getPlugin(), this::writeQueued, 100L);
                    break;
            }
        }
    }

    /**
     * Gets the <code>wait_timeout</code> variable and runs a repeating task (that sends requests to the database periodically as per the timeout) to keep the connection alive.
     * This method must be called asynchronously.
     */
    private void keepConnection() {
        //wait_timeout variable
        int wait_timeout;

        try {
            //Check the connection
            if (!connected || connection == null || connection.isClosed())
                return;

            //Get prepared statement
            PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.WAIT_TIMEOUT).getStatement(ScoreboardType.RIGHT));
            //Get the wait_timeout variable using query
            ResultSet resultSet = preparedStatement.executeQuery();

            //Shift to first row
            resultSet.next();
            //Set the value
            wait_timeout = resultSet.getInt("Value");

            //Close
            preparedStatement.close();

            //Info
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "Obtained the wait_timeout variable successfully (SUCCESS, " + wait_timeout + ").");
        } catch (SQLException | NullPointerException ex) {
            //Check the exception
            if (!checkException(ex)) {
                //Severe
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while obtaining the 'wait_timeout' variable from the database server (ERR_OTHER)! Using value 1s.", ex);
                //Set to 1 sec, which is minimum value in wait_timeout
                wait_timeout = 1;
            } else {
                //Return if connection failed, this method will be called again when reconnected
                return;
            }
        }

        //Start the connection keeper
        connectionKeeper = new ConnectionKeeper(wait_timeout);
    }

    /**
     * Creates scoreboard tables in the database.
     * This method must be called asynchronously.
     *
     * @return if the operation was successful
     */
    private boolean createTables() {
        try {
            //If not connected
            if (!isConnected())
                return false;

            //Create all tables
            for (ScoreboardType scoreboardType : ScoreboardType.values())
                createTable(scoreboardType);

            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.INFO, "All database tables created successfully (SUCCESS).");
        } catch (Exception ex) {
            //Check the exception type
            if (!checkException(ex))
                //Log
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while creating tables in the database (ERR_OTHER)! Please use /cps reload to reload.", ex);
            return false;
        }

        //Success
        return true;
    }

    /**
     * Creates a table for a scoreboard of the specified type.
     *
     * @param scoreboardType the scoreboard type
     * @throws SQLException an exception thrown during the execution
     * @throws NullPointerException an exception thrown during the execution
     */
    void createTable(ScoreboardType scoreboardType) throws SQLException, NullPointerException {
        //Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.CREATE_TABLE).getStatement(scoreboardType));
        //Create table
        preparedStatement.executeUpdate();
        //Close the statement
        preparedStatement.close();
    }

    /**
     * Drops a table of a scoreboard of the specified type.
     *
     * @param scoreboardType the scoreboard type
     * @throws SQLException an exception thrown during the execution
     * @throws NullPointerException an exception thrown during the execution
     */
    void dropTable(ScoreboardType scoreboardType) throws SQLException, NullPointerException {
        //Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.DROP_TABLE).getStatement(scoreboardType));
        //Create table
        preparedStatement.executeUpdate();
        //Close the statement
        preparedStatement.close();
    }

    @Override
    public OperationResult write(TestRecord testRecord) {
        return writeAll(new LinkedList<>(Collections.singletonList(testRecord)), null, false);
    }

    @Override
    public OperationResult writeAll(Queue<TestRecord> testRecords) {
        return writeAll(testRecords, null, false);
    }

    @Override
    public OperationResult writeAll(Queue<TestRecord> testRecords, WriteCallback writeCallback) {
        return writeAll(testRecords, writeCallback, false);
    }

    /**
     * Writes the given test records into the database and returns the result of this operation.
     * If not connected to the database or reformatting process is active (and if not called by reformatting thread), queues test records to be written after the operation finishes.
     * <p></p>
     * This method must be called asynchronously and is only for internal use.
     *
     * @param testRecords    records to write
     * @param writeCallback  optional callback to use as messenger
     * @param reformatThread if called by a reformatting thread (bypasses data format check)
     * @return the result of this operation
     * @see #write(TestRecord)
     */
    OperationResult writeAll(Queue<TestRecord> testRecords, WriteCallback writeCallback, boolean reformatThread) {
        //Result to be returned at the end of the method
        OperationResult operationResult = OperationResult.SUCCESS;
        //Repeating task instance
        RunnableTask repeating = null;
        //Record that is currently being written (for re-inserting in case of an exception)
        TestRecord current = null;
        //Written records
        ObjectInt written = new ObjectInt(0);

        try {
            //If null or an empty queue
            if (testRecords == null || testRecords.size() == 0)
                //Return success
                return operationResult;
            //Format checks
            if (!reformatThread && (!Reformatter.getInstance().canPerformOperations() || reformatActive))
                //Return
                return OperationResult.ERR_REFORMAT;
            //If not connected
            if (!isConnected()) {
                //Queue if not called by the reformatting thread
                if (!reformatThread)
                    queued.addAll(testRecords);
                //Return
                return OperationResult.ERR_DISCONNECT;
            }

            //Get the scoreboard type from the first element
            ScoreboardType scoreboardType = new ArrayList<>(testRecords).get(0).getScoreboardType();
            //Prepare one statement which will be reused then
            PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.WRITE).getStatement(scoreboardType));

            //Schedule messenger
            repeating = repeatingWriteMessenger(writeCallback, written, testRecords.size());

            //Loop through all
            while (testRecords.size() > 0) {
                //Get the current one
                current = testRecords.poll();
                //Write the record (insert or update if exists - this can't happen in HACK scoreboard, because ID - key is AUTO_INCREMENT)
                prepareStatement(preparedStatement,
                        current.getUuid().toString(), current.getCPS(), current.getMillis(),
                        current.getCPS(), current.getMillis())
                        .executeUpdate();

                //Increase
                written.change(1);
            }

            //Close the statement
            preparedStatement.close();
        } catch (SQLException | NullPointerException ex) {
            //Add the last record back
            if (current != null)
                testRecords.add(current);

            //Check the exception type
            if (checkException(ex)) {
                //Queue if not called by the reformatting thread
                if (!reformatThread)
                    queued.addAll(testRecords);
                //Set the result
                operationResult = OperationResult.ERR_DISCONNECT;
            } else {
                //Log
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while writing data into the database (ERR_OTHER)!", ex);
                //Set the result
                operationResult = OperationResult.ERR_OTHER;
            }
        }

        //Cancel the task
        if (repeating != null && !repeating.isCancelled())
            repeating.cancel();
        //Send 100% message if writing succeeded
        if (operationResult == OperationResult.SUCCESS && writeCallback != null)
            writeCallback.message(written.get(), written.get(), 100);

        return operationResult;
    }

    /**
     * Schedules a repeating task sending a message using {@link WriteCallback#message(long, long, int)} (per callback's delay).
     * If callback is <code>null</code>, returns <code>null</code>.
     *
     * @param writeCallback the callback
     * @param written       amount of written records (the value is changed by the caller)
     * @param contentSize   content size (total amount of records to be written)
     * @return the repeating task instance or <code>null</code> if the callback is <code>null</code>
     */
    public static RunnableTask repeatingWriteMessenger(WriteCallback writeCallback, ObjectInt written, int contentSize) {
        //If not null
        if (writeCallback != null) {
            //Schedule status messenger
            return new RunnableTask() {
                @Override
                public void run() {
                    //Writing
                    writeCallback.message(written.get(), contentSize, written.get() * 100 / contentSize);

                    //Send only once if repeating disabled, cancel now
                    if (writeCallback.getDelay() == -1 && !isCancelled())
                        cancel();
                }
            }.runTimer(ClicksPerSecond.getPlugin(), 0L, writeCallback.getDelay());
        }

        return null;
    }

    /**
     * Sets all given objects to parameter positions in the given prepared statement in the order as they are specified.
     *
     * @param statement the statement instance
     * @param toSet     the array of objects to be set in the statement's string
     * @return the given prepared statement instance with set parameters (if there are any)
     * @throws SQLException an exception thrown during the execution
     */
    private PreparedStatement prepareStatement(PreparedStatement statement, Object... toSet) throws SQLException {
        //Set all strings (no other types are used)
        for (int i = 1; i <= toSet.length; i++)
            statement.setObject(i, toSet[i - 1]);

        //Return finished statement
        return statement;
    }

    @Override
    public DataGetResult getAllData(ScoreboardType scoreboardType) {
        try {
            //Format checks
            if (!Reformatter.getInstance().canPerformOperations() || reformatActive)
                return new DataGetResult(OperationResult.ERR_REFORMAT, null);
            //If not connected
            if (!isConnected())
                return new DataGetResult(OperationResult.ERR_DISCONNECT, null);

            //Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.GET_ALL).getStatement(scoreboardType));
            //Get all rows
            ResultSet result = preparedStatement.executeQuery();

            //Create a list to return
            List<TestRecord> scoreboard = new ArrayList<>();

            //If getting HACK scoreboard
            if (scoreboardType == ScoreboardType.HACK)
                //Loop through all rows
                while (result.next())
                    //Add to the list
                    scoreboard.add(new TestRecord(ScoreboardType.HACK,
                            result.getInt("id"),
                            UUIDFactory.fromString(result.getString("uuid")),
                            result.getDouble("cps"),
                            result.getLong("t")));
            else
                //Loop through all rows
                while (result.next())
                    //Add to the list
                    scoreboard.add(new TestRecord(scoreboardType,
                            null,
                            UUIDFactory.fromString(result.getString("uuid")),
                            result.getDouble("cps"),
                            result.getLong("t")));

            //Close the statement
            preparedStatement.close();

            //Order by CPS or ID (depending on scoreboard type)
            scoreboard.sort(Comparator.reverseOrder());

            //Set places in scoreboard if not HACK scoreboard
            if (scoreboardType != ScoreboardType.HACK)
                //Set places
                for (int index = 0; index < scoreboard.size(); index++)
                    scoreboard.get(index).setPlace(index + 1);

            return new DataGetResult(OperationResult.SUCCESS, scoreboard);
        } catch (SQLException | NullPointerException ex) {
            //Check the exception type
            if (!checkException(ex)) {
                //Log
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while reading data from the database (ERR_OTHER)!", ex);
                //Return
                return new DataGetResult(OperationResult.ERR_OTHER, null);
            } else {
                //Return
                return new DataGetResult(OperationResult.ERR_DISCONNECT, null);
            }
        }
    }

    /**
     * Returns the description of a table that corresponds to the specified scoreboard.
     *
     * @param scoreboardType type of the scoreboard representing the table to describe
     * @return the description of the table
     */
    public List<ColumnDescription> getDescription(ScoreboardType scoreboardType) {
        try {
            //Return if not connected or if the reformatting process is active
            if (!isConnected() || reformatActive)
                return null;

            //Create list to return
            List<ColumnDescription> descriptions = new ArrayList<>();

            //Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.DESCRIBE).getStatement(scoreboardType));
            //Get description
            ResultSet result = preparedStatement.executeQuery();

            //Loop through all column descriptions
            while (result.next())
                //Add to the list
                descriptions.add(new ColumnDescription(
                        result.getString("Field"),
                        result.getString("Type"),
                        result.getString("Null"),
                        result.getString("Key"),
                        result.getString("Default"),
                        result.getString("Extra")
                ));

            //Close the statement
            preparedStatement.close();

            return descriptions;
        } catch (Exception ex) {
            //Check the exception type
            if (!checkException(ex))
                //Severe
                ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while reading table description from the database (ERR_OTHER)! Please use /cps reload to reload.", ex);
        }

        //Return null
        return null;
    }

    @Override
    public synchronized void reformat(ReformatCallback reformatCallback) {
        //Phase ID
        int phaseId = -1;
        //Last scoreboard reformatted
        ScoreboardType lastScoreboard = null;

        try {
            //Return if not connected
            if (!isConnected()) {
                reformatCallback.ERROR_DATABASE_disconnected();
                return;
            }

            //Get reformatter
            Reformatter reformatter = Reformatter.getInstance();
            //If reformat is not needed
            if (!reformatter.isReformatNeeded())
                return;

            //Process is active
            reformatActive = false;

            //Loop through scoreboards that need to be reformatted
            for (ScoreboardType scoreboardType : new HashSet<>(reformatter.getToReformat())) {
                //Last scoreboard
                lastScoreboard = scoreboardType;

                //Getting data
                reformatCallback.PHASE_getting(scoreboardType);
                //Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.GET_ALL).getStatement(scoreboardType));
                //Get everything
                ResultSet resultSet = preparedStatement.executeQuery();

                //Reformatting
                reformatCallback.PHASE_reformatting(scoreboardType);
                //Reformat
                List<TestRecord> formatted = reformatter.reformatDatabase(scoreboardType, resultSet);
                //Clear and add to the queue in case of any failure
                reformatQueue.clear();
                reformatQueue.addAll(formatted);
                //Close the statement
                preparedStatement.close();

                //Increment phase ID to 0 (first data-risky phase)
                phaseId++;
                //Recreating table
                reformatCallback.PHASE_DATABASE_recreatingTable(scoreboardType);
                //Drop table
                dropTable(scoreboardType);

                //Phase passed
                phaseId++;
                //Create table
                createTable(scoreboardType);

                //Phase passed
                phaseId++;
                //Content size
                int size = formatted.size();
                //Operation result
                OperationResult operationResult = OperationResult.SUCCESS;

                //If empty scoreboard
                if (size == 0) {
                    //Call to inform the sender
                    reformatCallback.PHASE_skipping(scoreboardType);
                } else {
                    //Write all
                    operationResult = writeAll(reformatQueue, new WriteCallback(reformatCallback.getMessageDelay()) {
                        @Override
                        public void message(long written, long total, int percent) {
                            //Writing
                            reformatCallback.PHASE_DATABASE_writing(scoreboardType, written, size, percent);
                        }
                    }, true);
                }

                //If not successful
                if (operationResult != OperationResult.SUCCESS) {
                    //Create post executor
                    reformatFailPostExecutor = new ReformatFailPostExecutor(scoreboardType, reformatQueue, ReformatFailPostExecutor.FailPhase.WRITE, reformatCallback);
                    //Send messages
                    reformatFailPostExecutor.mainErrorMessage(operationResult == OperationResult.ERR_DISCONNECT, null, reformatCallback);
                    reformatFailPostExecutor.postErrorMessage(operationResult == OperationResult.ERR_DISCONNECT, reformatCallback);

                    //Process is not active
                    reformatActive = false;
                    return;
                }

                //Reset phase ID
                phaseId = -1;

                //Update the version
                reformatter.updateVersion(scoreboardType);
                //Finished reformatting this scoreboard
                reformatCallback.PHASE_finished(scoreboardType);
            }

            //Finished
            reformatCallback.PHASE_finished();
        } catch (Exception ex) {
            //Check the exception
            boolean disconnected = checkException(ex);

            //Log the error
            ReformatFailPostExecutor.mainErrorMessage(disconnected, ex, reformatCallback);

            //If not in data-risky phase
            if (phaseId == -1) {
                //Not in data-risky stage
                reformatCallback.ERROR_DATABASE_POST_restart();
            } else {
                //Create post executor
                reformatFailPostExecutor = new ReformatFailPostExecutor(lastScoreboard, reformatQueue, ReformatFailPostExecutor.FailPhase.getById(phaseId), reformatCallback);
                //Send message
                reformatFailPostExecutor.postErrorMessage(disconnected, reformatCallback);
            }
        }

        //Process is not active
        reformatActive = false;
    }

    @Override
    public boolean isReformatActive() {
        return reformatActive;
    }

    /**
     * Reloads internal connection data.
     */
    public void reload() {
        //Configuration
        FileConfiguration config = ClicksPerSecond.getConfiguration();

        //Driver variables
        host = config.getString("database.host");
        port = config.getInt("database.port");
        database = config.getString("database.database");
        username = config.getString("database.username");
        password = config.getString("database.password");
        sslMode = config.getString("database.sslMode");

        //Timeouts
        reconnectTimeout = config.getInt("database.timeout.reconnect");
        connectTimeout = config.getInt("database.timeout.connect");
        socketTimeout = config.getInt("database.timeout.socket");

        //Tables to use
        for (ScoreboardType table : ScoreboardType.values())
            tables.put(table, config.getString("database.table." + table.getName()));

        //Initialize statement strings
        statements = new Statements(tables);

        //Check reconnect delay
        if (reconnectTimeout < 0) {
            //Set to 0
            reconnectTimeout = 0;
            //Warn
            ClicksPerSecond.getPlugin().getLogger().log(Level.WARNING, "Database reconnect delay is smaller than 0s! Using 0s for instant reconnecting.");
        }
    }

    /**
     * Checks the exception thrown during a database operation. Returns if thrown due to connection failure.
     * If <code>true</code>, automatically calls {@link #connect()}. If <code>false</code>, error should be logged by the calling method.
     *
     * @param ex the thrown exception
     * @return if the exception was thrown due to connection failure
     */
    synchronized boolean checkException(Exception ex) {
        //If not connected
        if (!isConnected())
            return true;

        //If this is exception saying connection was closed
        if ((ex instanceof SQLException && ex.getMessage().contains("No operations allowed after connection closed"))
                || (ex.getClass().getName().contains("CommunicationsException") && ex.getMessage().contains("Communications link failure"))) {

            //Stop the connection keeper
            if (connectionKeeper != null) {
                //Cancel
                connectionKeeper.cancel();
                //Set to null
                connectionKeeper = null;
            }

            //Close the connection
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            //Not connected
            connected = false;
            //Set connection to null
            connection = null;

            //Log
            ClicksPerSecond.getPlugin().getLogger().log(Level.SEVERE, "An error occurred while executing last database operation, connection has been unexpectedly closed (ERR_DISCONNECT)! Trying to connect to the database server again in " + reconnectTimeout + "s.", ex);
            //Try to reconnect
            Bukkit.getScheduler().runTaskLaterAsynchronously(ClicksPerSecond.getPlugin(), this::connect, reconnectTimeout * 20L);

            //Return true, exception was thrown due to connection failure
            return true;
        }

        //Return false, the exception was thrown due to some other thing
        return false;
    }

    /**
     * Returns if connected to the database server.
     *
     * @return if connected to the database server
     */
    public boolean isConnected() {
        return connection != null && connected;
    }

    /**
     * Returns the class instance.
     *
     * @return the class instance
     */
    public static Database getInstance() {
        return ClicksPerSecond.getDatabase();
    }

    /**
     * Class operating repeating task that keeps the connection alive.
     */
    private class ConnectionKeeper {

        //Cancel boolean
        private boolean cancel = false;
        //Executor instance
        private final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        //Task instance
        private ScheduledFuture<?> scheduledTask;

        //wait_timeout variable
        private long wait_timeout;
        //Prepared statement to be reused
        private PreparedStatement preparedStatement;

        /**
         * Starts the repeating task keeping the connection alive with the given delay.
         *
         * @param wait_timeout delay between connection keeping queries
         */
        private ConnectionKeeper(long wait_timeout) {
            //Copy
            this.wait_timeout = wait_timeout;

            //Prepare a statement
            try {
                this.preparedStatement = connection.prepareStatement(statements.getGroup(Statements.Type.SELECT_VOID).getStatement(ScoreboardType.RIGHT));
            } catch (SQLException ex) {
                //Handle the exception
                checkException(ex);
            }

            //Start
            run();
        }

        /**
         * Runs the repeating asynchronous task that sends requests to the database.
         */
        private void run() {
            scheduledTask = EXECUTOR.scheduleAtFixedRate(() -> {
                //Cancel if disconnected
                if (!connected) {
                    //Self cancel
                    scheduledTask.cancel(true);
                    cancel = true;
                }

                try {
                    //Run query and close immediately
                    preparedStatement.executeQuery().close();
                } catch (SQLException | NullPointerException ex) {
                    //Handle exception
                    checkException(ex);
                }
            }, 0L, (wait_timeout - 3 < 1 ? 100 : (wait_timeout - 3) * 1000), TimeUnit.MILLISECONDS);
        }

        /**
         * Cancels the repeating task.
         */
        private void cancel() {
            //Check if null, or cancelled already
            if (scheduledTask == null || cancel)
                return;

            //Cancel
            scheduledTask.cancel(true);
            cancel = true;
        }
    }
}
