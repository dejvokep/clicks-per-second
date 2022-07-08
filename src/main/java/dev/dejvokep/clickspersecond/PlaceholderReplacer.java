package dev.dejvokep.clickspersecond;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class PlaceholderReplacer {

    private final ClicksPerSecond plugin;
    private String unknownValue;
    private SimpleDateFormat dateFormat;

    public PlaceholderReplacer(ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    public String player(UUID uuid, String message) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return message.replace("{uuid}", uuid.toString())
                .replace("{name}", player.getName() == null ? unknownValue : player.getName())
                .replace("{id}", player.getName() == null ? uuid.toString() : player.getName());
    }

    public String info(PlayerInfo info, String message) {
        return player(info.getUniqueId(), message)
                .replace("{cps_best}", String.valueOf(info.getCPS()))
                .replace("{cps_best_date_millis}", String.valueOf(info.getTime()))
                .replace("{cps_best_date_formatted}", dateFormat.format(new Date(info.getTime())));
    }

    public String all(Sampler sampler, String message) {
        return info(sampler.getInfo(), message).replace("{cps_now}", String.valueOf(sampler.getCPS()));
    }

    public String all(Player player, String message) {
        return all(plugin.getClickHandler().getSampler(player.getUniqueId()), message);
    }

    public void reload() {
        this.unknownValue = plugin.getConfiguration().getString("placeholder.unknown-value");
        this.dateFormat = new SimpleDateFormat(plugin.getConfiguration().getString("placeholder.date-format"));
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public String getUnknownValue() {
        return unknownValue;
    }

}