package com.alliance.quizmanagement.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

/**
 * High-precision quiz timer utilizing ScheduledExecutorService on a background thread.
 * Addresses CO-3: Multithreading and Concurrency in Java.
 * Safely marshals tick updates and deadline triggers to the Swing Event Dispatch Thread (EDT).
 */
public class QuizTimer {

    public interface TimerListener {
        void onTick(long remainingSeconds, String formattedTime);
        void onTimeout();
    }

    private final long durationSeconds;
    private final TimerListener listener;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;
    private long deadlineEpochMillis;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    public QuizTimer(long durationSeconds, TimerListener listener) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Quiz duration must be strictly positive");
        }
        this.durationSeconds = durationSeconds;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Quiz-Timer-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (isRunning.getAndSet(true)) {
            return; // already running
        }
        this.deadlineEpochMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Schedule periodic 1-second ticks
        this.timerHandle = scheduler.scheduleAtFixedRate(this::checkDeadlineAndTick, 0, 1, TimeUnit.SECONDS);
    }

    private void checkDeadlineAndTick() {
        if (isFinished.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        long remainingMillis = deadlineEpochMillis - now;
        long remainingSeconds = Math.max(0, remainingMillis / 1000L);

        String formatted = formatTime(remainingSeconds);

        if (remainingSeconds <= 0) {
            if (!isFinished.getAndSet(true)) {
                stop();
                if (listener != null) {
                    SwingUtilities.invokeLater(listener::onTimeout);
                }
            }
        } else {
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.onTick(remainingSeconds, formatted));
            }
        }
    }

    public synchronized void stop() {
        isRunning.set(false);
        if (timerHandle != null && !timerHandle.isCancelled()) {
            timerHandle.cancel(true);
        }
        scheduler.shutdown();
    }

    public static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isFinished() {
        return isFinished.get();
    }

    public long getElapsedSeconds() {
        long now = System.currentTimeMillis();
        long startEpochMillis = deadlineEpochMillis - (durationSeconds * 1000L);
        return Math.max(0, (now - startEpochMillis) / 1000L);
    }
}
