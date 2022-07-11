package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Sampler {

    PlayerInfo info;

    public Sampler(@NotNull PlayerInfo info) {
        this.info = info;
    }

    @Nullable
    public abstract PlayerInfo addClick();

    @Nullable
    public abstract PlayerInfo close();

    public abstract int getCPS();

    @Nullable
    public PlayerInfo setInfo(@NotNull PlayerInfo info) {
        return this.info = info;
    }

    public void setFetchedInfo(@NotNull PlayerInfo info) {
        // Set
        this.info = this.info.setAll(Math.max(this.info.getCPS(), info.getCPS()), info.getCPS() >= this.info.getCPS() ? info.getTime() : this.info.getTime(), info.getFetchTime());
    }

    @NotNull
    public PlayerInfo getInfo() {
        return info;
    }
}