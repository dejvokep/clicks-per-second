package dev.dejvokep.clickspersecond.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Placeholders {

    private static final boolean PAPI_AVAILABLE = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

    public static String set(Player player, String text) {
        return PAPI_AVAILABLE ? PlaceholderAPI.setPlaceholders(player, text) : text;
    }

}