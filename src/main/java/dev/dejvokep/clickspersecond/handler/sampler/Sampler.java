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