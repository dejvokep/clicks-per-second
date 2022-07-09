package dev.dejvokep.clickspersecond.utils.watcher;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Watchers {

    private final Map<Player, Player> watchers = new HashMap<>(), watched = new HashMap<>();

    @Nullable
    public Player start(@NotNull Player watcher, @NotNull Player watched) {
        Player previous = watchers.remove(watcher);
        this.watchers.put(watcher, watched);
        this.watched.put(watched, watcher);
        return previous;
    }

    @Nullable
    public Player stop(@NotNull Player watcher) {
        Player watched = watchers.remove(watcher);
        if (watched != null)
            this.watched.remove(watched);
        return watched;
    }

    @Nullable
    public Player getWatched(@NotNull Player watcher) {
        return watchers.get(watcher);
    }

    @Nullable
    public Player getWatcher(@NotNull Player watched) {
        return this.watched.get(watched);
    }

}