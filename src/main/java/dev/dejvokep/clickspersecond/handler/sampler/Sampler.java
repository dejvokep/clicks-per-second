package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.PlayerInfo;

public abstract class Sampler {

    PlayerInfo info;
    private boolean initialFetch = false;

    public Sampler(PlayerInfo info) {
        this.info = info;
    }

    public abstract PlayerInfo addClick();
    public abstract PlayerInfo close();
    public abstract int getCPS();

    public PlayerInfo setInfo(PlayerInfo info) {
        return this.info = info;
    }

    public void setFetchedInfo(PlayerInfo info) {
        // Set
        this.info = this.info.setAll(Math.max(this.info.getCPS(), info.getCPS()), info.getCPS() >= this.info.getCPS() ? info.getTime() : this.info.getTime(), !initialFetch && !this.info.isToggleUpdated() && info.getToggle() || this.info.getToggle(), info.getFetchTime());
        initialFetch = true;
    }

    public PlayerInfo getInfo() {
        return info;
    }
}