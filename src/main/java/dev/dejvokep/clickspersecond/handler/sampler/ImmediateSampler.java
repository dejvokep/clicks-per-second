package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.player.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ImmediateSampler extends Sampler {

    private final Queue<Long> queue = new LinkedBlockingQueue<>();
    private int previous = 0;

    public ImmediateSampler(@NotNull PlayerInfo info) {
        super(info);
    }

    @Nullable
    public PlayerInfo addClick() {
        // Time
        long time = System.currentTimeMillis();
        // Add click
        queue.add(time);

        // Store
        int prev = previous;
        // Reset
        this.previous = queue.size();

        // If going down from peak and the peak was more than the best
        if (queue.size() < prev && prev > info.getCPS())
            return setInfo(info.setCPS(prev, time));

        // Nothing new
        return null;
    }

    @Override
    @Nullable
    public PlayerInfo close() {
        return previous > info.getCPS() ? info.setCPS(previous, System.currentTimeMillis()) : null;
    }

    public int getCPS() {
        return queue.size();
    }

    public void clear() {
        long time = System.currentTimeMillis() - 1000;
        while (queue.size() > 0 && queue.peek() < time)
            queue.poll();
    }

}