package gg.itzkatze.mcc.scoreboard;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.game.GameTimer;
import gg.itzkatze.mcc.player.MccPlayer;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class ScoreboardManager {

    private static final String SIDEBAR_OBJECTIVE = "challenge_info";
    private static final Map<MccPlayer, Scoreboard> SCOREBOARDS = new HashMap<>();
    private static final Map<MccPlayer, Map<Integer, Component>> LAST_LINES = new HashMap<>();

    public static void startUpdater() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), ScoreboardManager::updateAll, 20L, 20L);
    }

    public static void updateAll() {
        boolean gameActive = ChallengeManager.isGameStarted()
                && (MinecraftChallenges.getTimer().getMode() == GameTimer.Mode.DISABLED
                    || MinecraftChallenges.getTimer().isRunning());

        for (MccPlayer mccPlayer : MccPlayer.getOnlinePlayers()) {
            boolean shouldHaveScoreboard = !mccPlayer.isExcluded() && gameActive;
            boolean showScoreboard = SettingsManager.isEnabled(GlobalSetting.SHOW_SCOREBOARD);

            if (shouldHaveScoreboard && showScoreboard) {
                updateScoreboard(mccPlayer);
            } else if (SCOREBOARDS.containsKey(mccPlayer)) {
                removeSidebar(mccPlayer);
            }
        }
    }

    public static Scoreboard getOrCreateScoreboard(MccPlayer player) {
        return SCOREBOARDS.computeIfAbsent(player, p -> Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public static @Nullable Scoreboard findScoreboard(MccPlayer player) {
        return SCOREBOARDS.get(player);
    }

    public static void updateScoreboard(MccPlayer player) {
        Set<Challenge> activeChallenges = ChallengeManager.getActiveChallenges();
        if (activeChallenges.isEmpty()) return;

        // Find the challenge with the highest priority
        Challenge highestPriorityChallenge = activeChallenges.stream()
                .filter(c -> c.getScoreboardContent() != null)
                .max(Comparator.comparingInt(c -> c.getScoreboardContent().getPriority()))
                .orElse(null);
        if (highestPriorityChallenge == null) return;

        ScoreboardContent content = highestPriorityChallenge.getScoreboardContent();
        if (content == null) return;

        Scoreboard scoreboard = getOrCreateScoreboard(player);

        Objective obj = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(
                    SIDEBAR_OBJECTIVE, "dummy",
                    styleTitle(content.getTitle())
            );
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Map<Integer, Component> last = LAST_LINES.computeIfAbsent(player, p -> new HashMap<>());
        Map<Integer, Component> newLines = new HashMap<>();

        // Get lines from the challenge's scoreboard content
        Component[] lines = content.getLines(player);

        // Add lines from top to bottom (scoreboard shows from bottom to top)
        int lineNumber = 15;
        for (int i = 0; i < lines.length && lineNumber > 0; i++) {
            if (lines[i] != null && !lines[i].equals(Component.empty())) {
                newLines.put(lineNumber--, lines[i]);
            }
        }

        // Remove old lines that are no longer present
        for (Map.Entry<Integer, Component> entry : last.entrySet()) {
            if (!newLines.containsKey(entry.getKey())) {
                scoreboard.resetScores(lineKey(entry.getKey()));
            }
        }

        // Add new lines
        for (Map.Entry<Integer, Component> entry : newLines.entrySet()) {
            if (!Objects.equals(entry.getValue(), last.get(entry.getKey()))) {
                Score score = obj.getScore(lineKey(entry.getKey()));
                score.customName(styleLine(entry.getValue()));
                score.numberFormat(NumberFormat.blank());
                score.setScore(entry.getKey());
            }
        }

        LAST_LINES.put(player, newLines);

        if (player.getPlayer().getScoreboard() != scoreboard) {
            player.getPlayer().setScoreboard(scoreboard);
        }
    }

    public static void removeSidebar(MccPlayer player) {
        LAST_LINES.remove(player);
        Scoreboard scoreboard = SCOREBOARDS.get(player);
        if (scoreboard == null) return;

        Objective sidebar = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
        if (sidebar != null) {
            sidebar.unregister();
        }
    }

    private static Component styleTitle(Component title) {
        return title.colorIfAbsent(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
    }

    private static Component styleLine(Component line) {
        return line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static String lineKey(int lineNumber) {
        return "line_" + lineNumber;
    }
}
