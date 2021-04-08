package com.davidcubesvk.clicksPerSecond.utils.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Class based on {@link BukkitTask}, implements {@link #isCancelled()} to be available in all server versions.
 */
public abstract class RunnableTask implements Runnable {

    //If the task is cancelled
    private boolean cancelled = false;
    //BukkitTask instance for cancelling
    private BukkitTask task;

    /**
     * Runs the task asynchronously.
     *
     * @param plugin the plugin running the task
     * @return the task instance
     */
    public RunnableTask runAsync(Plugin plugin) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                //Run the run() method
                RunnableTask.this.run();
            }
        }.runTaskAsynchronously(plugin);

        return this;
    }

    /**
     * Runs the task later.
     *
     * @param plugin the plugin running the task
     * @param delay  the delay to wait for before running the task in ticks
     * @return the task instance
     * @see BukkitRunnable#runTaskLater(Plugin, long)
     */
    public RunnableTask runLater(Plugin plugin, long delay) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                //Run the run() method
                RunnableTask.this.run();
            }
        }.runTaskLater(plugin, delay);

        return this;
    }

    /**
     * Runs the task later asynchronously.
     *
     * @param plugin the plugin running the task
     * @param delay  the delay to wait for before running the task in ticks
     * @return the task instance
     * @see BukkitRunnable#runTaskLaterAsynchronously(Plugin, long)
     */
    public RunnableTask runLaterAsync(Plugin plugin, long delay) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                //Run the run() method
                RunnableTask.this.run();
            }
        }.runTaskLaterAsynchronously(plugin, delay);

        return this;
    }

    /**
     * Runs the task timer.
     *
     * @param plugin the plugin running the task
     * @param delay  the delay to wait for before the first run in ticks
     * @param period the delay between each run in ticks
     * @return the task instance
     * @see BukkitRunnable#runTaskTimer(Plugin, long, long)
     */
    public RunnableTask runTimer(Plugin plugin, long delay, long period) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                //Run the run() method
                RunnableTask.this.run();
            }
        }.runTaskTimer(plugin, delay, period);

        return this;
    }

    /**
     * Runs the task timer asynchronously.
     *
     * @param plugin the plugin running the task
     * @param delay  the delay to wait for before the first run in ticks
     * @param period the delay between each run in ticks
     * @return the task instance
     * @see BukkitRunnable#runTaskTimerAsynchronously(Plugin, long, long)
     */
    public RunnableTask runTimerAsync(Plugin plugin, long delay, long period) {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                //Run the run() method
                RunnableTask.this.run();
            }
        }.runTaskTimerAsynchronously(plugin, delay, period);

        return this;
    }

    /**
     * Cancels the task.
     */
    public void cancel() {
        //If not cancelled
        if (!cancelled)
            //Cancel the task
            task.cancel();

        //Set cancelled to true
        cancelled = true;
    }

    /**
     * Returns if the task is cancelled.
     *
     * @return if the task is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

}
