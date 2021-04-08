package com.davidcubesvk.clicksPerSecond.test;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.utils.action.Action;
import com.davidcubesvk.clicksPerSecond.utils.replacer.PlayerStringReplacer;
import com.davidcubesvk.clicksPerSecond.utils.task.RunnableTask;
import org.bukkit.entity.Player;

/**
 * Permanent action sender.
 */
class PermanentAction {

    //Task instance for cancelling
    private RunnableTask task;

    /**
     * Sends the given actions every 1 tick to a player until the {@link #cancel()} method is called.
     * <p></p>
     * Optional replacers can be used to replace placeholders in actions.
     *
     * @param player    the receiver
     * @param action    the actions to send
     * @param replacers the optional placeholder replacers
     */
    PermanentAction(Player player, Action action, PlayerStringReplacer... replacers) {
        task = new RunnableTask() {
            public void run() {
                //Run
                action.run(player, replacers);
            }
        }.runTimer(ClicksPerSecond.getPlugin(), 0L, 1L);
    }

    /**
     * Cancels (stops) the repeating task sending actions.
     */
    void cancel() {
        //Cancel if the task is not null and has not been cancelled
        if (task != null && !task.isCancelled())
            task.cancel();
    }

}
