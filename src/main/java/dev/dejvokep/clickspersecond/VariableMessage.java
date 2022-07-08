package dev.dejvokep.clickspersecond;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public class VariableMessage<T> {

    private final Watchers watchers;
    private final T normal, watching;

    private VariableMessage(ClicksPerSecond plugin, T normal, T watching) {
        this.watchers = plugin.getWatchers();
        this.normal = normal;
        this.watching = watching;
    }

    public T get(Player player) {
        return get(player, (message, target) -> message);
    }

    public <R> R get(Player player, BiFunction<T, Player, R> mapper) {
        Player watched = watchers.getWatched(player);
        T message = watched == null ? normal : watching;
        return mapper.apply(message, watched == null ? player : watched);
    }

    public static <T> VariableMessage<T> of(ClicksPerSecond plugin, T normal, T watching) {
        return new VariableMessage<>(plugin, normal, watching);
    }

    public static VariableMessage<String> of(ClicksPerSecond plugin, Section config) {
        return new VariableMessage<>(plugin, ChatColor.translateAlternateColorCodes('&', config.getString("normal")), ChatColor.translateAlternateColorCodes('&', config.getString("watching")));
    }

}