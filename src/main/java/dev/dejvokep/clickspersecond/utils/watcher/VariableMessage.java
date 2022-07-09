package dev.dejvokep.clickspersecond.utils.watcher;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class VariableMessage<T> {

    private final Watchers watchers;
    private final T normal, watching;

    private VariableMessage(@NotNull ClicksPerSecond plugin, @NotNull T normal, @NotNull T watching) {
        this.watchers = plugin.getWatchers();
        this.normal = normal;
        this.watching = watching;
    }

    @NotNull
    public T get(@NotNull Player player) {
        return get(player, (message, target) -> message);
    }

    @NotNull
    public <R> R get(@NotNull Player player, @NotNull BiFunction<T, Player, R> mapper) {
        Player watched = watchers.getWatched(player);
        T message = watched == null ? normal : watching;
        return mapper.apply(message, watched == null ? player : watched);
    }

    @NotNull
    public static <T> VariableMessage<T> of(@NotNull ClicksPerSecond plugin, @NotNull T normal, @NotNull T watching) {
        return new VariableMessage<>(plugin, normal, watching);
    }

    @NotNull
    public static VariableMessage<String> of(@NotNull ClicksPerSecond plugin, @NotNull Section config) {
        return new VariableMessage<>(plugin, ChatColor.translateAlternateColorCodes('&', config.getString("normal")), ChatColor.translateAlternateColorCodes('&', config.getString("watching")));
    }

}