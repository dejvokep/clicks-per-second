package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.container.PlayerContainer;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * An abstract class for click and CPS handlers.
 */
public abstract class ClickHandler<T extends Sampler> implements PlayerContainer {

    private final Map<UUID, T> samplers;
    private final ClicksPerSecond plugin;

    /**
     * Initializes the handler.
     *
     * @param plugin   the plugin
     * @param samplers the sampler map
     */
    public ClickHandler(@NotNull ClicksPerSecond plugin, @NotNull Map<UUID, T> samplers) {
        this.plugin = plugin;
        this.samplers = samplers;
    }

    @Override
    public void add(@NotNull Player player) {
        // Add
        samplers.put(player.getUniqueId(), createSampler(player));
        // Fetch
        plugin.getDataStorage().queueFetch(player.getUniqueId());
    }

    @Override
    public void remove(@NotNull Player player) {
        // If absent
        if (!samplers.containsKey(player.getUniqueId()))
            return;

        // Remove
        PlayerInfo info = samplers.remove(player.getUniqueId()).close();
        // Update
        if (info != null)
            plugin.getDataStorage().update(info);
    }

    /**
     * Creates sampler for the given player. Called when adding a player to the handler.
     *
     * @param player the player
     * @return the created sampler
     */
    protected abstract T createSampler(@NotNull Player player);

    /**
     * Sets the newly fetched info to the appropriate {@link Sampler} for caching during player connection lifetime.
     *
     * @param info the info to set
     */
    public void setFetchedInfo(@NotNull PlayerInfo info) {
        Sampler sampler = getSampler(info.getUniqueId());
        if (sampler != null)
            sampler.setFetchedInfo(info);
    }

    /**
     * Returns the info cached by the appropriate {@link Sampler}, or <code>null</code> if the {@link Player} is not
     * online.
     *
     * @param uuid the ID
     * @return the info, if any
     */
    @Nullable
    public PlayerInfo getInfo(@NotNull UUID uuid) {
        Sampler sampler = getSampler(uuid);
        return sampler == null ? null : sampler.getInfo();
    }

    /**
     * Returns sampler of {@link Player} represented by the given ID, or <code>null</code> if the {@link Player} is not
     * online.
     *
     * @param uuid the ID
     * @return the sampler, if any
     */
    @Nullable
    public Sampler getSampler(@NotNull UUID uuid) {
        return samplers.get(uuid);
    }

    /**
     * Processes the given player's click.
     *
     * @param player the player
     */
    public void processClick(@NotNull Player player) {
        // Add click
        PlayerInfo updated = samplers.get(player.getUniqueId()).addClick();
        // No update
        if (updated == null)
            return;

        // Update
        plugin.getDataStorage().update(updated);
    }

    /**
     * Returns the CPS of the given player, or <code>-1</code> if not online.
     *
     * @param player the player
     * @return CPS of the given player
     */
    public int getCPS(@NotNull Player player) {
        Sampler sampler = getSampler(player.getUniqueId());
        return sampler == null ? -1 : sampler.getCPS();
    }

    /**
     * Returns the smallest display refresh rate (delay between refreshes) which will be able to reflect CPS changes by
     * the currently used {@link Sampler}.
     *
     * @return the min display refresh rate
     */
    public abstract int getMinDisplayRate();

    /**
     * Returns the samplers.
     *
     * @return the samplers
     */
    protected Map<UUID, T> getSamplers() {
        return samplers;
    }

}