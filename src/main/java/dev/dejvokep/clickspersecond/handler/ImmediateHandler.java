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
import dev.dejvokep.clickspersecond.handler.sampler.ImmediateSampler;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ClickHandler} which has instant CPS sampling with queues.
 */
public class ImmediateHandler extends ClickHandler<ImmediateSampler> {

    /**
     * Initializes the handler.
     *
     * @param plugin the plugin
     */
    public ImmediateHandler(@NotNull ClicksPerSecond plugin) {
        super(plugin, new ConcurrentHashMap<>());

        new BukkitRunnable() {
            @Override
            public void run() {
                while (true)
                    getSamplers().forEach((player, sampler) -> sampler.clear());
            }
        }.runTask(plugin);
    }

    @Override
    protected ImmediateSampler createSampler(@NotNull Player player) {
        return new ImmediateSampler(PlayerInfo.initial(player.getUniqueId()));
    }

    @Override
    public int getMinDisplayRate() {
        return 1;
    }

}