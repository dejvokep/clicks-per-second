package com.davidcubesvk.clicksPerSecond.utils.data.reformatter;

import java.util.UUID;

/**
 * An API class used to get {@link UUID} instance from a string.
 */
public class UUIDFactory {

    /**
     * Converts UUID in string format to an object accepting both dashed and non-dashed UUID strings.
     *
     * @param uuid the UUID string
     * @return the UUID object
     */
    public static UUID fromString(String uuid) {
        return UUID.fromString(uuid.length() == 36 ? uuid : uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20));
    }

}
