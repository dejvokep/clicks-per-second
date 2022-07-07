package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerContainer;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ClickHandler extends PlayerContainer {

    void reload(int rate);

    void setFetchedInfo(PlayerInfo info);
    PlayerInfo getInfo(UUID uuid);
    Sampler getSampler(UUID uuid);

    void processClick(Player player);
    int getCPS(Player player);

    int getDisplayRate();

}