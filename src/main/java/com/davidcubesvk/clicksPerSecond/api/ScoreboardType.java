package com.davidcubesvk.clicksPerSecond.api;

/**
 * Enum representing scoreboard types.
 * - RIGHT: right-click mode scoreboard
 * - LEFT: left-click mode scoreboard
 * - HACK: hacking scoreboard
 */
public enum ScoreboardType {

    RIGHT, LEFT, HACK;

    /**
     * Returns the name of this enum constant, but in lower-case.
     *
     * @return the lower-cased name of this enum constant
     */
    public String getName() {
        return name().toLowerCase();
    }

}
