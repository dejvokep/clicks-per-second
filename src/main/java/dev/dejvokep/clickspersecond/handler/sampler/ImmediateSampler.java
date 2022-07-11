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

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implementation of {@link Sampler} which has instant CPS sampling with queues.
 */
public class ImmediateSampler extends Sampler {

    // Click queue (by time)
    private final Queue<Long> queue = new LinkedBlockingQueue<>();
    // Previous CPS
    private int previous = 0;

    /**
     * Initializes the sampler with the given initial info.
     *
     * @param info the initial info
     */
    public ImmediateSampler(@NotNull PlayerInfo info) {
        super(info);
    }

    @Override
    @Nullable
    public PlayerInfo addClick() {
        // Time
        long time = System.currentTimeMillis();
        // Add click
        queue.add(time);

        // Store
        int prev = previous;
        // Reset
        this.previous = queue.size();

        // If going down from peak and the peak was more than the best
        if (queue.size() < prev && prev > info.getCPS())
            return setInfo(info.setCPS(prev, time));

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
        return queue.size();
    }

    /**
     * Removes outdated clicks (older than 1 second) from the click queue.
     */
    public void clear() {
        long time = System.currentTimeMillis() - 1000;
        while (queue.size() > 0 && queue.peek() < time)
            queue.poll();
    }

}