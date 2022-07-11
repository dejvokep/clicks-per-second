/*
 * Copyright 2022 https://dejvokep.dev/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dejvokep.clickspersecond.utils.player;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * An immutable object containing player information.
 */
public class PlayerInfo {

    private final UUID uuid;
    private final int cps;
    private final long time, fetchTime;
    private final boolean loading;

    /**
     * Initializes as initial player information, which is subject to a pending data storage request, hence {@link
     * #isLoading()} will always return <code>true</code>.
     *
     * @param uuid owner ID
     */
    private PlayerInfo(@NotNull UUID uuid) {
        this.uuid = uuid;
        this.cps = 0;
        this.time = 0;
        this.fetchTime = 0;
        this.loading = true;
    }

    /**
     * Initializes as fully loaded player information.
     *
     * @param uuid      owner ID
     * @param cps       CPS
     * @param time      time at which the CPS were achieved (or <code>0</code> if <code>0</code>)
     * @param fetchTime time at which this info was fetched
     */
    private PlayerInfo(@NotNull UUID uuid, int cps, long time, long fetchTime) {
        this.uuid = uuid;
        this.cps = cps;
        this.time = time;
        this.fetchTime = fetchTime;
        this.loading = false;
    }

    /**
     * Creates new information with the given CPS and time and all other properties inherited from this instance.
     *
     * @param cps  the CPS
     * @param time time at which the CPS were achieved
     * @return the new object with modified properties
     */
    public PlayerInfo setCPS(int cps, long time) {
        return setAll(cps, time, fetchTime);
    }

    /**
     * Creates new information with the given properties. The new object will not inherit any properties from this
     * instance.
     *
     * @param cps       the CPS
     * @param time      time at which the CPS were achieved
     * @param fetchTime time at which this info was fetched
     * @return the new object with modified properties
     */
    public PlayerInfo setAll(int cps, long time, long fetchTime) {
        return new PlayerInfo(uuid, cps, time, fetchTime);
    }

    /**
     * Initializes as initial player information, which is subject to a pending data storage request, hence {@link
     * #isLoading()} will always return <code>true</code>.
     *
     * @param uuid owner ID
     */
    public static PlayerInfo initial(@NotNull UUID uuid) {
        return new PlayerInfo(uuid);
    }

    /**
     * Initializes as fully loaded, but empty player information.
     *
     * @param uuid owner ID
     */
    public static PlayerInfo empty(@NotNull UUID uuid) {
        return new PlayerInfo(uuid, 0, 0, System.currentTimeMillis());
    }

    /**
     * Initializes as fully loaded player information.
     *
     * @param uuid owner ID
     * @param cps  CPS
     * @param time time at which the CPS were achieved (or <code>0</code> if <code>0</code>)
     */
    public static PlayerInfo from(@NotNull UUID uuid, int cps, long time) {
        return new PlayerInfo(uuid, cps, time, System.currentTimeMillis());
    }

    /**
     * Returns the owning ID of this information.
     *
     * @return the owning ID
     */
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Returns the CPS. Value of <code>0</code> means no data (player has not clicked yet).
     *
     * @return the CPS
     */
    public int getCPS() {
        return cps;
    }

    /**
     * Returns the time at which the {@link #getCPS() best CPS} were achieved, or <code>0</code> if <code>{@link
     * #getCPS()} == 0</code>.
     *
     * @return the time at which the record was achieved, or <code>0</code> if <code>{@link #getCPS()} == 0</code>
     */
    public long getTime() {
        return time;
    }

    /**
     * Returns the time at which this information was fetched.
     *
     * @return the time at which this information was fetched
     */
    public long getFetchTime() {
        return fetchTime;
    }

    /**
     * Returns whether the owner of this information has CPS displays toggled on or off. <b>This API is subject to an
     * update in the future, if requested, and always returns <code>true</code>.</b>
     *
     * @return always <code>true</code>
     */
    public boolean getToggle() {
        return true;
    }

    /**
     * Returns whether this is initial information, subject to a pending data storage request.
     *
     * @return if the information is loading
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Returns whether this is empty information, e.g. if <code>{@link #getCPS()} == {@link #getTime()} == 0</code>.
     *
     * @return if this information is empty
     */
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