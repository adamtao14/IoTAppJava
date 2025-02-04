package com.example.iotapp.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeriodicTask {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void scheduleTask(Runnable task, int times, long interval, Runnable callback) {
        Runnable periodicTask = new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count < times) {
                    task.run(); // Execute the task
                    count++;
                } else {
                    scheduler.shutdown(); // Stop the scheduler after `x`
                    if (callback != null) {
                        callback.run(); // Notify that the task is finished
                    }
                }
            }
        };

        // Schedule the task to run every `y` minutes
        scheduler.scheduleWithFixedDelay(periodicTask, 0, interval, TimeUnit.MINUTES);
    }

}