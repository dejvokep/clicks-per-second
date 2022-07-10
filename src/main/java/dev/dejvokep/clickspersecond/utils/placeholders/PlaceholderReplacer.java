package dev.dejvokep.clickspersecond.utils.placeholders;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.clickspersecond.ClicksPerSecond;
import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class PlaceholderReplacer {

    public static final boolean PLACEHOLDER_API_AVAILABLE = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    private final ClicksPerSecond plugin;
    private String unknownValue;
    private SimpleDateFormat dateFormat;
    private boolean allowPlaceholderApi;

    public PlaceholderReplacer(@NotNull ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    @NotNull
    public String player(@NotNull UUID uuid, @NotNull String message) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return message.replace("{uuid}", uuid.toString())
                .replace("{name}", player.getName() == null ? unknownValue : player.getName())
                .replace("{id}", player.getName() == null ? uuid.toString() : player.getName());
    }

    @NotNull
    public String player(@NotNull Player player, @NotNull String message) {
        return message.replace("{uuid}", player.getUniqueId().toString())
                .replace("{name}", player.getName())
                .replace("{id}", player.getName());
    }

    @NotNull
    public String info(@NotNull PlayerInfo info, @NotNull String message) {
        return player(info.getUniqueId(), message)
                .replace("{cps_best}", String.valueOf(info.getCPS()))
                .replace("{cps_best_date_millis}", String.valueOf(info.getTime()))
                .replace("{cps_best_date_formatted}", dateFormat.format(new Date(info.getTime())));
    }

    @NotNull
    public String all(@NotNull Sampler sampler, @NotNull String message) {
        return info(sampler.getInfo(), message).replace("{cps_now}", String.valueOf(sampler.getCPS()));
    }

    @NotNull
    public String all(@NotNull Player player, @NotNull String message) {
        return all(Objects.requireNonNull(plugin.getClickHandler().getSampler(player.getUniqueId())), message);
    }

    @NotNull
    public String api(@NotNull Player player, @NotNull String message) {
        message = all(Objects.requireNonNull(plugin.getClickHandler().getSampler(player.getUniqueId())), message);
        return allowPlaceholderApi && PLACEHOLDER_API_AVAILABLE ? PlaceholderAPI.setPlaceholders(player, message) : message;
    }

    public void reload() {
        Section config = plugin.getConfiguration().getSection("placeholder");
        this.unknownValue = config.getString("unknown-value");
        this.dateFormat = new SimpleDateFormat(config.getString("date-format"));
        this.allowPlaceholderApi = config.getBoolean("allow-placeholder-api");
    }

    @NotNull
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    @NotNull
    public String getUnknownValue() {
        return unknownValue;
    }

}