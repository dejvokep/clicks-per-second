package dev.dejvokep.clickspersecond.utils.container;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerContainer {

    void add(@NotNull Player player);
    void remove(@NotNull Player player);
    void removeAll();

}