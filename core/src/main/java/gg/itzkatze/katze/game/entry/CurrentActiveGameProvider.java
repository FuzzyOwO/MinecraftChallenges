package gg.itzkatze.katze.game.entry;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.GameTimer;
import net.kyori.adventure.text.Component;

import java.util.List;

public final class CurrentActiveGameProvider implements ActiveGameProvider {
    private final GameEntryService entries;

    public CurrentActiveGameProvider(GameEntryService entries) {
        this.entries = entries;
    }

    @Override
    public List<ActiveGameView> activeGames() {
        GameTimer timer = KatzeGames.getTimer();
        if (!ChallengeManager.isGameStarted() || entries.currentRoundId() == null || entries.roundFinished()) return List.of();
        List<ChallengeType> modes = ChallengeManager.getActiveChallenges().stream().map(Challenge::getType).sorted().toList();
        if (modes.isEmpty()) return List.of();
        boolean paused = timer.getMode() != GameTimer.Mode.DISABLED && !timer.isRunning();
        Component phase = Component.translatable(paused ? "katze.game.limbo.paused" : "katze.game.limbo.running");
        return List.of(new ActiveGameView(entries.currentRoundId(), modes, phase, entries.playingCount()));
    }
}
