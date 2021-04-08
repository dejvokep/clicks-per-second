package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.StorageType;
import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.test.TestManager;
import com.davidcubesvk.clicksPerSecond.utils.data.database.Database;
import com.davidcubesvk.clicksPerSecond.utils.updater.Updater;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Handler for reload sub-command.
 */
public class Reload implements CommandProcessor {

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        Bukkit.getScheduler().runTaskAsynchronously(ClicksPerSecond.getPlugin(), () -> {
            //Check the permission and args
            if (!commandUtil.hasPermission("cps.command.reload") || !commandUtil.checkArgs(1, 1))
                return;

            //Reloading
            commandUtil.sendMessage("command.main.reload.reloading");

            //Reload files
            ClicksPerSecond.reload();
            //Reload API
            ClicksPerSecondAPI.getInstance().reload();
            //Reload manager
            TestManager.getInstance().reload();
            //Reload updater
            Updater.getInstance().reload();
            //Reload test command
            ClicksPerSecond.getTestCommandExecutor().reload();

            //If database is used currently
            if (ClicksPerSecond.getStorageType() == StorageType.DATABASE) {
                //Database class instance
                Database database = Database.getInstance();

                //Disconnect
                database.disconnect();
                //Reload
                database.reload();
                //Connect
                database.connect();
            }

            //Reloaded
            commandUtil.sendMessage("command.main.reload.reloaded");
        });
    }

}
