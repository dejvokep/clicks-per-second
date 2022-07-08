package dev.dejvokep.clickspersecond;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Watchers {

    private final Map<Player, Player> watchers = new HashMap<>();

    public Player start(Player watcher, Player watched) {
        Player previous = watchers.remove(watcher);
        watchers.put(watcher, watched);
        return previous;
    }

    public Player stop(Player watcher) {
        return watchers.remove(watcher);
    }

    public Player getWatched(Player watcher) {
        return watchers.get(watcher);
    }

}