package dev.dejvokep.clickspersecond.handler;

import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.RatedSampler;
import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Implementation of {@link ClickHandler} which has rated CPS sampling.
 */
public class RatedHandler extends ClickHandler<RatedSampler> {

    private final int rate;

    /**
     * Initializes the handler.
     *
     * @param plugin the plugin
     * @param rate   the sampling rate
     */
    public RatedHandler(@NotNull ClicksPerSecond plugin, int rate) {
        super(plugin, new HashMap<>());
        this.rate = rate;

        // Schedule
        Bukkit.getScheduler().runTaskTimer(plugin, () -> getSamplers().forEach((uuid, sampler) -> {
            // Reset
            PlayerInfo updated = sampler.reset();
            // Update
            if (updated != null)
                plugin.getDataStorage().update(updated);
        }), rate, rate);
    }

    @Override
    protected RatedSampler createSampler(@NotNull Player player) {
        return new RatedSampler(rate, PlayerInfo.initial(player.getUniqueId()));
    }

    @Override
    public int getMinDisplayRate() {
        return rate;
    }

}