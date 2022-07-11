package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract sampler class used to manage and sample player CPS.
 */
public abstract class Sampler {

    // Cached info
    protected PlayerInfo info;

    /**
     * Initializes the sampler with the given initial info.
     *
     * @param info the initial info
     */
    public Sampler(@NotNull PlayerInfo info) {
        this.info = info;
    }

    /**
     * Adds click to the sampler and returns new information needed to upload to the data storage, if any.
     *
     * @return the information to upload, if any
     */
    @Nullable
    public abstract PlayerInfo addClick();

    /**
     * Closes the sampler and returns new information needed to upload to the data storage, if any.
     *
     * @return the information to upload, if any
     */
    @Nullable
    public abstract PlayerInfo close();

    /**
     * Returns non-negative sampled CPS.
     *
     * @return the sampled CPS
     */
    public abstract int getCPS();

    /**
     * Sets the given info for the sampler and returns it.
     *
     * @param info the info to set
     * @return the given info
     */
    @Nullable
    public PlayerInfo setInfo(@NotNull PlayerInfo info) {
        return this.info = info;
    }

    /**
     * Sets the given fetched info to the sampler. Runs CPS comparisons to not overwrite better CPS results.
     *
     * @param info the fetched info
     */
    public void setFetchedInfo(@NotNull PlayerInfo info) {
        this.info = this.info.setAll(Math.max(this.info.getCPS(), info.getCPS()), info.getCPS() >= this.info.getCPS() ? info.getTime() : this.info.getTime(), info.getFetchTime());
    }

    /**
     * Returns the cached info with the latest data.
     *
     * @return the cached info
     */
    @NotNull
    public PlayerInfo getInfo() {
        return info;
    }
}