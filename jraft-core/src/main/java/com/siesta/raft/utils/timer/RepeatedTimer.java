package com.siesta.raft.utils.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author hujiaofen
 * @since 2/6/2022
 */
public abstract class RepeatedTimer {
    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    protected RepeatedTimer(int poolSize) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize);
    }

    protected abstract void onTrigger();

    protected abstract int adjustTimout();

    public void start() {
        this.cancel();
        this.schedule();
    }

    private void schedule() {
        this.scheduledFuture = this.scheduledExecutorService.schedule(() -> {
            onTrigger();
            schedule();
        }, adjustTimout(), TimeUnit.MILLISECONDS);
    }

    public void cancel() {
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(true);
        }
    }
}
