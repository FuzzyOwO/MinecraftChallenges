package gg.itzkatze.mcc.challenges;

import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface Challenge extends ChallengeSettings {

    /**
     * Called when the challenge is activated/loaded.
     */
    default void onEnable() {
        // Default empty implementation
    }

    /**
     * Called when the challenge is deactivated/unloaded.
     */
    default void onDisable() {
        // Default empty implementation
    }

    /**
     * Called when a configured game is explicitly started.
     */
    default void onGameStart() {
        // Default empty implementation
    }

    /**
     * Called when a started game is reset back into its pre-start state.
     */
    default void onGameReset() {
        // Default empty implementation
    }

    /**
     * Timer behavior requested by this challenge for the current game session.
     * Return null to indicate this challenge does not require timer control.
     */
    @Nullable default TimerSpec getTimerSpec() {
        return null;
    }

    /**
     * Called when the configured countdown timer reaches zero.
     */
    default void onTimerElapsed() {
        // Default empty implementation
    }

    /**
     * Gets the name/identifier of this challenge.
     * @return The challenge name
     */
    @NotNull String getName();

    /**
     * Gets the display name of this challenge for UI purposes.
     * @return The display name
     */
    @NotNull Component getDisplayName();

    @NotNull ChallengeType getType();

    /**
     * Gets the material/icon used to represent this challenge in GUIs.
     * @return The material to use for this challenge's icon
     */
    @NotNull default Material getMaterial() {
        return Material.BOOK; // Default fallback material
    }

    /**
     * Gets the description of this challenge for UI purposes.
     * @return The description text
     */
    @NotNull default String getDescription() {
        return "A Minecraft challenge"; // Default description
    }

    /**
     * Gets all commands specific to this challenge.
     * @return List of commands
     */
    @NotNull List<MccCommand> getCommands();

    /**
     * Gets all event listeners specific to this challenge.
     * @return List of listeners
     */
    @NotNull List<Listener> getListeners();

    /**
     * Gets all tasks that should run for this challenge.
     * @return List of tasks
     */
    @NotNull List<Runnable> getTasks();

    /**
     * Gets a challenge-specific actionbar message.
     * Return null to let the default timer actionbar render instead.
     */
    @Nullable default Component getActionbar(@NotNull MccPlayer player, boolean timerRunning, int timerSeconds) {
        return null;
    }

    /**
     * Gets the priority of this challenge's actionbar override when multiple challenges are active.
     * Higher priority wins.
     */
    default int getActionbarPriority() {
        return 0;
    }

    /**
     * Gets the scoreboard content for this challenge.
     * @return The scoreboard content, or null if no scoreboard content
     */
    @Nullable default ScoreboardContent getScoreboardContent() {
        return null; // Default implementation returns null
    }

    /**
     * Gets all bossbar contents for this challenge.
     * Each BossbarContent handles one bossbar; use multiple entries when multiple bossbars are needed.
     * @return The list of bossbar contents (empty if no bossbars)
     */
    @NotNull default List<BossbarContent> getBossbarContents() {
        return Collections.emptyList();
    }
}
