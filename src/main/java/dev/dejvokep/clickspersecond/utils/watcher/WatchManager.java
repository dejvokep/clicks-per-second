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
package dev.dejvokep.clickspersecond.utils.watcher;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Watch manager storing information about watching relations.
 */
public class WatchManager {

    // Watchers
    private final Map<Player, Player> watchers = new HashMap<>();

    /**
     * Starts a watching session and returns the previously watched player of the given watcher, if any,
     * <code>null</code> otherwise.
     *
     * @param watcher the watcher
     * @param watched the watched player
     * @return previously watched player, if any
     */
    @Nullable
    public Player start(@NotNull Player watcher, @NotNull Player watched) {
        Player previous = watchers.remove(watcher);
        this.watchers.put(watcher, watched);
        return previous;
    }

    /**
     * Stops watching session of the given watcher and returns the watched player, or <code>null</code> if the watcher
     * did not watch anyone.
     *
     * @param watcher the watcher
     * @return the watched player, if any
     */
    @Nullable
    public Player stop(@NotNull Player watcher) {
        return watchers.remove(watcher);
    }

    /**
     * Returns the player who is watched by the given watcher, if any.
     *
     * @param watcher the watcher
     * @return the watched player
     */
    @Nullable
    public Player getWatched(@NotNull Player watcher) {
        return watchers.get(watcher);
    }

    /**
     * Consumes all watchers who are watching the given player.
     *
     * @param watched         the watched player
     * @param watcherConsumer consumer of the watchers
     */
    public void consumeWatchers(@NotNull Player watched, @NotNull Consumer<Player> watcherConsumer) {
        for (Map.Entry<Player, Player> entry : watchers.entrySet())
            if (entry.getValue() == watched)
                watcherConsumer.accept(entry.getKey());
    }

}