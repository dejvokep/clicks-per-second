package dev.dejvokep.clickspersecond.utils.watcher;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Watchers {

    private final Map<Player, Player> watchers = new HashMap<>(), watched = new HashMap<>();

    public Player start(Player watcher, Player watched) {
        Player previous = watchers.remove(watcher);
        this.watchers.put(watcher, watched);
        this.watched.put(watched, watcher);
        return previous;
    }

    public Player stop(Player watcher) {
        Player watched = watchers.remove(watcher);
        if (watched != null)
            this.watched.remove(watched);
        return watched;
    }

    public Player getWatched(Player watcher) {
        return watchers.get(watcher);
    }

    public Player getWatcher(Player watched) {
        return this.watched.get(watched);
    }

}