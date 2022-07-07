package dev.dejvokep.clickspersecond;

import dev.dejvokep.clickspersecond.handler.sampler.Sampler;
import dev.dejvokep.clickspersecond.utils.PlayerInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlaceholderReplacer {

    private final ClicksPerSecond plugin;
    private String unknownValue;
    private SimpleDateFormat dateFormat;

    public PlaceholderReplacer(ClicksPerSecond plugin) {
        this.plugin = plugin;
        reload();
    }

    public String replace(PlayerInfo info, String message) {
        return message.replace("{uuid}", info.getUniqueId().toString())
                .replace("{best_cps}", String.valueOf(info.getCPS()))
                .replace("{best_date_millis}", String.valueOf(info.getTime()))
                .replace("{best_date_formatted}", dateFormat.format(new Date(info.getTime())));
    }

    public String replace(Sampler sampler, String message) {
        return replace(sampler.getInfo(), message).replace("{cps}", String.valueOf(sampler.getCPS()));
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