package dev.dejvokep.clickspersecond.handler.sampler;

import dev.dejvokep.clickspersecond.utils.PlayerInfo;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ImmediateSampler extends Sampler {

    private final Queue<Long> queue = new LinkedBlockingQueue<>();
    private int previous = 0;

    public ImmediateSampler(PlayerInfo info) {
        super(info);
    }

    public PlayerInfo addClick() {
        // Add click
        queue.add(System.currentTimeMillis());

        // Store
        int prev = previous;
        // Reset
        this.previous = queue.size();

        // If going down from peak and the peak was more than the best
        if (queue.size() < prev && prev > info.getCPS())
            return setInfo(info.setCPS(prev, System.currentTimeMillis()));

        // Nothing new
        return null;
    }

    @Override
    public PlayerInfo close() {
        return previous > info.getCPS() ? info.setCPS(previous, System.currentTimeMillis()) : null;
    }

    public int getCPS() {
        return queue.size();
    }

    public void clear() {
        while (queue.size() > 0 && queue.peek() < System.currentTimeMillis() - 1000)
            queue.poll();
    }

}