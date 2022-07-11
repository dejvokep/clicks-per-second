/*
 * Copyright 2022 https://dejvokep.dev/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link Sampler} which has rated CPS sampling.
 */
public class RatedSampler extends Sampler {

    // Clicks
    private int clicks = 0, cps = 0, previous = 0;
    // Rate in seconds
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

    @Override
    public void resetCPS() {
        clicks = cps = previous = 0;
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