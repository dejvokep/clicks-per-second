package dev.dejvokep.clickspersecond;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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

    public String replace(UUID uuid, String message) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return message.replace("{uuid}", uuid.toString()).replace("{name}", player.getName() == null ? unknownValue : player.getName());
    }

    public String replace(PlayerInfo info, String message) {
        return replace(info.getUniqueId(), message)
                .replace("{cps_best}", String.valueOf(info.getCPS()))
                .replace("{cps_best_date_millis}", String.valueOf(info.getTime()))
                .replace("{cps_best_date_formatted}", dateFormat.format(new Date(info.getTime())));
    }

    public String replace(Sampler sampler, String message) {
        return replace(sampler.getInfo(), message).replace("{cps_now}", String.valueOf(sampler.getCPS()));
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