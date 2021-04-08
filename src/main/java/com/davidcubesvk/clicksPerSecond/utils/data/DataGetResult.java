package com.davidcubesvk.clicksPerSecond.utils.data;

import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;

import java.util.List;

/**
 * Stores data of a data-getting operation.
 */
public class DataGetResult {

    //Operation result
    private Database.OperationResult result;
    //Data
    private List<TestRecord> data;

    /**
     * Initializes this data-getting operation's result by the given data.
     *
     * @param result the result of the operation
     * @param data   the data got from the operation
     */
    public DataGetResult(Database.OperationResult result, List<TestRecord> data) {
        this.result = result;
        this.data = data;
    }

    /**
     * Returns the result of this data-getting operation.
     *
     * @return the result of this operation
     */
    public Database.OperationResult getResult() {
        return result;
    }

    /**
     * Returns the data got from this data-getting operation.
     * It is recommended to check {@link #getResult()} for errors before manipulating with these data, as they may be <code>null</code>.
     *
     * @return the data got from this operation
     */
    public List<TestRecord> getData() {
        return data;
    }
}
