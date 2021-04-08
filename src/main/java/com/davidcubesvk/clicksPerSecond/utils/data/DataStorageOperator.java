package com.davidcubesvk.clicksPerSecond.utils.data;

import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.ReformatCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.callback.WriteCallback;
import com.davidcubesvk.clicksPerSecond.utils.data.reformatter.Reformatter;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;

import java.util.Queue;

/**
 * Interface covering both database and file operator.
 */
public interface DataStorageOperator {

    /**
     * Writes the given test record instance into the data storage and returns the result of this operation.
     * If not connected to the database or reformatting process is active, queues the test record to be written once ready to write again.
     * This method must be called asynchronously (if this call refers to {@link Database} class).
     *
     * @param testRecord the record to write
     * @return the result of this operation
     * @see #writeAll(Queue)
     */
    Database.OperationResult write(TestRecord testRecord);

    /**
     * Writes the given test records (in a collection) into the data storage and returns the result of this operation.
     * If not connected to the database or reformatting process is active, queues the test records to be written once ready to write again.
     * All records must be targeted to the same scoreboard.
     * <p></p>
     * As this invokes a bulk data operation, this method must be called asynchronously.
     *
     * @param testRecords the records to write
     * @return the result of this operation
     * @see #writeAll(Queue, WriteCallback)
     */
    Database.OperationResult writeAll(Queue<TestRecord> testRecords);

    /**
     * Writes the given test records (in a collection) into the data storage and returns the result of this operation. During the operation, callback is used to notify about the progress.
     * If not connected to the database or reformatting process is active, queues the test records to be written once ready to write again.
     * All records must be targeted to the same scoreboard.
     * <p></p>
     * As this invokes a bulk data operation, this method must be called asynchronously.
     *
     * @param testRecords   the records to write
     * @param writeCallback the callback
     * @return the result of this operation
     */
    Database.OperationResult writeAll(Queue<TestRecord> testRecords, WriteCallback writeCallback);

    /**
     * Returns the scoreboard data and operation result in a result holder.
     *
     * @param scoreboardType type of the scoreboard to get
     * @return the result containing operation result and data
     */
    DataGetResult getAllData(ScoreboardType scoreboardType);

    /**
     * Reformats all data in the storage. Check {@link Reformatter#isReformatNeeded()} if it is needed to call this method.
     * Callback is used to notify about the progress.
     * <p></p>
     * As this invokes a bulk data operation, this method must be called asynchronously.
     *
     * @param reformatCallback the callback
     */
    void reformat(ReformatCallback reformatCallback);

    /**
     * Returns if the storage is being reformatted (if yes, returns <code>false</code>).
     *
     * @return if the storage is ready to perform any operation
     */
    boolean isReformatActive();

}
