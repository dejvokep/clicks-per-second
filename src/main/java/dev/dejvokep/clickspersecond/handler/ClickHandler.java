package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.container.PlayerContainer;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface for click and CPS handlers.
 */
public interface ClickHandler extends PlayerContainer {

    /**
     * Sets the newly fetched info to the appropriate {@link Sampler} for caching during player connection lifetime.
     * @param info the info to set
     */
    void setFetchedInfo(@NotNull PlayerInfo info);

    /**
     * Returns the info cached by the appropriate {@link Sampler}.
     * @param uuid the ID to get the info of
     * @return the info, or <code>null</code> if the {@link Player} represented by the ID is not online
     */
    @Nullable
    PlayerInfo getInfo(@NotNull UUID uuid);

    @Nullable
    Sampler getSampler(@NotNull UUID uuid);

    void processClick(@NotNull Player player);
    int getCPS(@NotNull Player player);

    int getDisplayRate();

}