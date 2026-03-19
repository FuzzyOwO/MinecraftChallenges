package gg.itzkatze.mcc.game;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {

    private static final Map<Challenge, List<Runnable>> tasksByChallenge = new HashMap<>();
    private static BukkitTask runningTask = null;

    public static void register(Challenge challenge, Runnable task) {
        tasksByChallenge.computeIfAbsent(challenge, key -> new ArrayList<>()).add(task);
    }

    public static void unregister(Challenge challenge) {
        tasksByChallenge.remove(challenge);
        if (tasksByChallenge.isEmpty()) {
            stopAll();
        }
    }

    public static void startAll() {
        if (tasksByChallenge.isEmpty()) {
            stopAll();
            return;
        }
        if (runningTask != null) {
            runningTask.cancel();
        }

        runningTask = Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), () -> {
            for (List<Runnable> tasks : tasksByChallenge.values()) {
                for (Runnable task : tasks) {
                    task.run();
                }
            }
        }, 0L, 3L);
    }

    public static void stopAll() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
        tasksByChallenge.clear();
    }
}
