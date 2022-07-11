package dev.dejvokep.clickspersecond.utils.player;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class PlayerInfo {

    private final UUID uuid;
    private final int cps;
    private final long time;
    private final long fetchTime;

    private final boolean loading;

    private PlayerInfo(@NotNull UUID uuid) {
        this.uuid = uuid;
        this.cps = 0;
        this.time = 0;
        this.fetchTime = 0;
        this.loading = true;
    }

    private PlayerInfo(@NotNull UUID uuid, int cps, long time) {
        this(uuid, cps, time, System.currentTimeMillis());
    }

    private PlayerInfo(@NotNull UUID uuid, int cps, long time, long fetchTime) {
        this.uuid = uuid;
        this.cps = cps;
        this.time = time;
        this.fetchTime = fetchTime;
        this.loading = false;
    }

    public PlayerInfo setCPS(int cps, long time) {
        return setAll(cps, time, fetchTime);
    }

    public PlayerInfo setAll(int cps, long time, long fetchTime) {
        return new PlayerInfo(uuid, cps, time, fetchTime);
    }

    public static PlayerInfo initial(@NotNull UUID uuid) {
        return new PlayerInfo(uuid);
    }
    public static PlayerInfo empty(@NotNull UUID uuid) {
        return new PlayerInfo(uuid, 0, 0);
    }
    public static PlayerInfo from(@NotNull UUID uuid, int cps, long time) {
        return new PlayerInfo(uuid, cps, time);
    }

    public long getFetchTime() {
        return fetchTime;
    }

    @NotNull
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
        return true;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isEmpty() {
        return cps == 0 && time == 0;
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