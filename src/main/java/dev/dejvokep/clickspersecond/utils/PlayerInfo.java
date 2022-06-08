package dev.dejvokep.clickspersecond.utils;

import java.util.Objects;
import java.util.UUID;

public class PlayerInfo {

    private final UUID uuid;
    private final int cps;
    private final long time;
    private final boolean toggle;
    private final long fetchTime;

    private final boolean loading, toggleUpdated;

    private PlayerInfo(UUID uuid) {
        this.uuid = uuid;
        this.cps = 0;
        this.time = 0;
        this.fetchTime = 0;
        this.toggle = false;
        this.loading = true;
        this.toggleUpdated = false;
    }

    private PlayerInfo(UUID uuid, int cps, long time, boolean toggle, boolean toggleUpdated) {
        this(uuid, cps, time, toggle, System.currentTimeMillis(), toggleUpdated);
    }

    private PlayerInfo(UUID uuid, int cps, long time, boolean toggle, long fetchTime, boolean toggleUpdated) {
        this.uuid = uuid;
        this.cps = cps;
        this.time = time;
        this.fetchTime = fetchTime;
        this.toggle = toggle;
        this.toggleUpdated = toggleUpdated;
        this.loading = false;
    }

    public PlayerInfo setCPS(int cps, long time) {
        return setAll(cps, time, toggle, fetchTime);
    }

    public PlayerInfo setAll(int cps, long time, boolean toggle, long fetchTime) {
        return new PlayerInfo(uuid, cps, time, toggle, fetchTime, toggleUpdated);
    }

    public static PlayerInfo initial(UUID uuid) {
        return new PlayerInfo(uuid);
    }
    public static PlayerInfo empty(UUID uuid) {
        return new PlayerInfo(uuid, 0, 0, true, false);
    }
    public static PlayerInfo from(UUID uuid, int cps, long time, boolean toggle) {
        return new PlayerInfo(uuid, cps, time, toggle, false);
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public int getCPS() {
        return cps;
    }

    public long getTime() {
        return time;
    }

    public boolean getToggle() {
        return toggle;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isToggleUpdated() {
        return toggleUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerInfo)) return false;
        PlayerInfo that = (PlayerInfo) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}