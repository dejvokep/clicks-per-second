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
package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.RatedSampler;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Implementation of {@link ClickHandler} which has rated CPS sampling.
 */
public class RatedHandler extends ClickHandler<RatedSampler> {

    // Rate in ticks
    private final int rate;

    /**
     * Initializes the handler.
     *
     * @param plugin the plugin
     * @param rate   the sampling rate
     */
    public RatedHandler(@NotNull ClicksPerSecond plugin, int rate) {
        super(plugin, new HashMap<>());
        this.rate = rate;

        // Schedule
        Bukkit.getScheduler().runTaskTimer(plugin, () -> getSamplers().forEach((uuid, sampler) -> {
            // Reset
            PlayerInfo updated = sampler.reset();
            // Update
            if (updated != null)
                plugin.getDataStorage().sync(updated);
        }), rate, rate);
    }

    @Override
    protected RatedSampler createSampler(@NotNull Player player) {
        return new RatedSampler(rate, PlayerInfo.initial(player.getUniqueId()));
    }

    @Override
    public int getMinDisplayRate() {
        return rate;
    }

}