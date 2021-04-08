package com.davidcubesvk.clicksPerSecond.event;

import com.davidcubesvk.clicksPerSecond.test.TestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Class with a listener listening to the interact event.
 */
public class Interact implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(PlayerInteractEvent event) {
        //Check if action is PHYSICAL
        if (event.getAction() == Action.PHYSICAL)
            return;

        //Add click
        TestManager.getInstance().addClick(event.getPlayer().getUniqueId(), event.getAction());
    }

}
