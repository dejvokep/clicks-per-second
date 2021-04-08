package com.davidcubesvk.clicksPerSecond.event;

import com.davidcubesvk.clicksPerSecond.test.Test;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Class with a listener listening to the quit event.
 */
public class Quit implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        //End if running any test
        TestManager.getInstance().endTest(event.getPlayer(), Test.EndCause.DISCONNECTED);
    }

}
