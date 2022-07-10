package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.container.PlayerContainer;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ClickHandler extends PlayerContainer {

    void setFetchedInfo(@NotNull PlayerInfo info);

    @Nullable
    PlayerInfo getInfo(@NotNull UUID uuid);

    @Nullable
    Sampler getSampler(@NotNull UUID uuid);

    void processClick(@NotNull Player player);
    int getCPS(@NotNull Player player);

    int getDisplayRate();

}