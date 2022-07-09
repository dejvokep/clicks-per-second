package dev.dejvokep.clickspersecond.utils.container;

import org.bukkit.entity.Player;

public interface PlayerContainer {

    void add(Player player);
    void remove(Player player);
    void removeAll();

}