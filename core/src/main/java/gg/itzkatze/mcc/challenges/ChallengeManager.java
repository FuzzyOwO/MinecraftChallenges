package gg.itzkatze.mcc.challenges;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CommandHandler;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.game.TaskManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChallengeManager {

    @Getter
    private static final Set<Challenge> activeChallenges = new HashSet<>();
    @Getter
    private static boolean gameStarted = false;

    /** Commands registered per challenge, so we can unregister them when the challenge is disabled. */
    private static final Map<Challenge, List<MccCommand>> commandsByChallenge = new HashMap<>();
    /** Listeners registered per challenge, so we can unregister them when the challenge is disabled. */
    private static final Map<Challenge, List<Listener>> listenersByChallenge = new HashMap<>();

    /**
     * Loads a challenge by activating it.
     * @param challenge The challenge to load
     */
    public static void loadChallenge(@NotNull Challenge challenge) {
        if (activeChallenges.contains(challenge)) {
            return; // Already loaded
        }

        activeChallenges.add(challenge);

        // Initialize the challenge
        challenge.onEnable();

        // Register commands and track for unregister on disable
        List<MccCommand> commands = new ArrayList<>(challenge.getCommands());
        commandsByChallenge.put(challenge, commands);
        for (MccCommand command : commands) {
            CommandHandler.register(command);
        }
        refreshCommandListForAllPlayers();

        // Register listeners
        List<Listener> listeners = new ArrayList<>(challenge.getListeners());
        listenersByChallenge.put(challenge, listeners);
        for (Listener listener : listeners) {
            Plugin plugin = MinecraftChallenges.get();
            MinecraftChallenges.get().getServer().getPluginManager().registerEvents(listener, plugin);
        }

        // Register tasks
        for (Runnable task : challenge.getTasks()) {
            TaskManager.register(challenge, task);
        }

        TaskManager.startAll();
    }

    /**
     * Unloads a specific challenge.
     * @param challenge The challenge to unload
     */
    public static void unloadChallenge(@NotNull Challenge challenge) {
        if (!activeChallenges.contains(challenge)) {
            return; // Not loaded
        }

        activeChallenges.remove(challenge);

        challenge.onDisable();

        List<MccCommand> commands = commandsByChallenge.remove(challenge);
        if (commands != null) {
            for (MccCommand command : commands) {
                MccCommand.unregister(command);
            }
        }
        refreshCommandListForAllPlayers();

        List<Listener> listeners = listenersByChallenge.remove(challenge);
        if (listeners != null) {
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }

        TaskManager.unregister(challenge);

        if (activeChallenges.isEmpty()) {
            gameStarted = false;
        }
    }

    /**
     * Unloads all challenges.
     */
    public static void unloadAllChallenges() {
        for (Challenge challenge : activeChallenges) {
            challenge.onDisable();
        }
        activeChallenges.clear();
        gameStarted = false;

        for (List<MccCommand> commands : commandsByChallenge.values()) {
            for (MccCommand command : commands) {
                MccCommand.unregister(command);
            }
        }
        commandsByChallenge.clear();

        for (List<Listener> listeners : listenersByChallenge.values()) {
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }
        listenersByChallenge.clear();

        refreshCommandListForAllPlayers();

        TaskManager.stopAll();
    }

    public static boolean startActiveChallenges() {
        if (activeChallenges.isEmpty() || gameStarted) {
            return false;
        }

        for (Challenge challenge : activeChallenges) {
            challenge.onGameStart();
        }
        gameStarted = true;
        return true;
    }

    public static void resetGameState() {
        gameStarted = false;
        for (Challenge challenge : activeChallenges) {
            challenge.onGameReset();
        }
    }

    /**
     * Resends the command list to all online players so the client shows the current
     * set of registered commands (e.g. after loading or disabling a challenge).
     */
    private static void refreshCommandListForAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    /**
     * Checks if a challenge type is currently active.
     * @param challengeType The challenge type to check
     * @return True if a challenge of this type is active
     */
    public static boolean isChallengeActive(@NotNull ChallengeType challengeType) {
        return activeChallenges.stream()
                .anyMatch(challenge -> challenge.getType() == challengeType);
    }
}
