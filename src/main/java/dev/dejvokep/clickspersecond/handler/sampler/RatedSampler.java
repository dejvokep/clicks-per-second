package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RatedSampler extends Sampler {

    private int clicks = 0, cps = 0, previous = 0;
    private final double rate;

    public RatedSampler(int tickRate, @NotNull PlayerInfo info) {
        super(info);
        rate = (double) tickRate / 20;
    }

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

    public int getCPS() {
        return cps;
    }

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