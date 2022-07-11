package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link Sampler} which has rated CPS sampling.
 */
public class RatedSampler extends Sampler {

    private int clicks = 0, cps = 0, previous = 0;
    private final double rate;

    /**
     * Initializes the sampler.
     *
     * @param tickRate sampling rate (length of each sampling period) in ticks
     * @param info     the initial info
     */
    public RatedSampler(int tickRate, @NotNull PlayerInfo info) {
        super(info);
        rate = (double) tickRate / 20;
    }

    @Override
    @Nullable
    public PlayerInfo addClick() {
        // Add click
        clicks++;
        // Nothing new
        return null;
    }

    @Override
    @Nullable
    public PlayerInfo close() {
        return previous > info.getCPS() ? info.setCPS(previous, System.currentTimeMillis()) : null;
    }

    @Override
    public int getCPS() {
        return cps;
    }

    /**
     * Resets the sampler to new sampling period.
     * <p>
     * If current CPS are lower than the previous, but higher than the record, {@link #setInfo(PlayerInfo) sets} and
     * returns the new info, which should be uploaded to the data storage. If there are no updates, returns
     * <code>null</code>.
     *
     * @return the information to upload, if any
     */
    @Nullable
    public PlayerInfo reset() {
        // Store
        int prev = previous;

        // CPS
        cps = (int) Math.round(clicks / rate);
        // Reset
        clicks = 0;
        this.previous = cps;


        // If going down from peak and the peak was more than the best
        if (cps < prev && prev > info.getCPS())
            return setInfo(info.setCPS(prev, System.currentTimeMillis()));

        // Nothing new
        return null;
    }

}