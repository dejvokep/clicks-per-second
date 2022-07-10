package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Sampler {

    PlayerInfo info;
    private boolean initialFetch = false;

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
        this.info = this.info.setAll(Math.max(this.info.getCPS(), info.getCPS()), info.getCPS() >= this.info.getCPS() ? info.getTime() : this.info.getTime(), !initialFetch && !this.info.isToggleUpdated() && info.getToggle() || this.info.getToggle(), info.getFetchTime());
        initialFetch = true;
    }

    @NotNull
    public PlayerInfo getInfo() {
        return info;
    }
}