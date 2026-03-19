package gg.itzkatze.mcc.bossbar;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.bossbar.BossBar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class BossbarManager {

    /** All bars shown per challenge. Cleared when challenge is disabled. */
    private static final Map<Challenge, Map<MccPlayer, Set<BossBar>>> barsByChallenge = new HashMap<>();

    /**
     * Hides all bossbars for the given challenge. Call from {@link Challenge#onDisable()}.
     */
    public static void hideBarsForChallenge(Challenge challenge) {
        hideAndClearBarsForChallenge(challenge, barsByChallenge);
    }

    private static void hideAndClearBarsForChallenge(
            Challenge challenge,
            Map<Challenge, Map<MccPlayer, Set<BossBar>>> byChallenge
    ) {
        Map<MccPlayer, Set<BossBar>> bars = byChallenge.remove(challenge);
        if (bars == null) return;
        for (Map.Entry<MccPlayer, Set<BossBar>> entry : bars.entrySet()) {
            MccPlayer player = entry.getKey();
            for (BossBar bar : entry.getValue()) {
                player.getAudience().hideBossBar(bar);
            }
        }
    }

    public static void runTask() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Set<Challenge> activeChallenges = ChallengeManager.getActiveChallenges();
                if (activeChallenges.isEmpty()) return;

                boolean timerRunning = MinecraftChallenges.getTimer().isRunning();
                BossbarKind showKind = timerRunning ? BossbarKind.RUNNING : BossbarKind.NOT_RUNNING;

                for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
                    if (player.isExcluded()) {
                        hideAllBarsForPlayer(player);
                        continue;
                    }

                    for (Challenge challenge : activeChallenges) {
                        clearChallengeBarsForPlayer(player, challenge);
                    }

                    for (Challenge challenge : activeChallenges) {
                        for (BossbarContent content : challenge.getBossbarContents()) {
                            if (content.getKind() != showKind) continue;
                            BossBar bar = content.getBossbar(player);
                            if (bar != null) {
                                addBossBar(player, bar, challenge);
                            }
                        }
                    }
                }
            }
        };
        new Timer("challenge/bossbar").scheduleAtFixedRate(task, 150, 150);
    }

    private static void hideAllBarsForPlayer(MccPlayer player) {
        for (Map<MccPlayer, Set<BossBar>> bars : barsByChallenge.values()) {
            Set<BossBar> set = bars.remove(player);
            if (set != null) {
                for (BossBar bar : set) {
                    player.getAudience().hideBossBar(bar);
                }
            }
        }
    }

    private static void clearChallengeBarsForPlayer(MccPlayer player, Challenge challenge) {
        Map<MccPlayer, Set<BossBar>> bars = barsByChallenge.get(challenge);
        if (bars == null) return;
        Set<BossBar> set = bars.remove(player);
        if (set != null) {
            for (BossBar bar : set) {
                player.getAudience().hideBossBar(bar);
            }
        }
    }

    private static void addBossBar(MccPlayer player, BossBar bar, Challenge challenge) {
        player.getAudience().showBossBar(bar);
        barsByChallenge
                .computeIfAbsent(challenge, k -> new HashMap<>())
                .computeIfAbsent(player, k -> new HashSet<>())
                .add(bar);
    }
}
