package gg.itzkatze.katze.game.entry;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.game.TimerRegistry;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActiveGamesGUITest {
    @Test void browserListsOneCombinedPausedRound() throws Exception {
        Set<Challenge> previous = new HashSet<>(ChallengeManager.getActiveChallenges());
        Field field = ChallengeManager.class.getDeclaredField("gameStarted");
        field.setAccessible(true);
        boolean started = field.getBoolean(null);
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode oldMode = timer.getMode();
        try {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().add(new Mode(ChallengeType.RANDOMIZER));
            ChallengeManager.getActiveChallenges().add(new Mode(ChallengeType.ITEM_RAIN));
            ChallengeManager.markGameStarted();
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            GameEntryService service = new GameEntryService();
            UUID round = UUID.randomUUID();
            service.beginRound(round, Set.of(UUID.randomUUID()));
            List<ActiveGameView> views = new CurrentActiveGameProvider(service).activeGames();
            assertEquals(1, views.size());
            assertEquals(round, views.getFirst().roundId());
            assertEquals(Set.of(ChallengeType.RANDOMIZER, ChallengeType.ITEM_RAIN), Set.copyOf(views.getFirst().modes()));
        } finally {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().addAll(previous);
            field.setBoolean(null, started);
            timer.setMode(oldMode);
        }
    }

    @Test void emptyBrowserIsSafe() {
        ActiveGamesGUI gui = new ActiveGamesGUI(List::of, new GameEntryService());
        gui.setItems(null);
        assertNotNull(gui.get(13));
    }
    @Test void completedRoundIsAbsent() throws Exception {
        Set<Challenge> previous = new HashSet<>(ChallengeManager.getActiveChallenges());
        Field field = ChallengeManager.class.getDeclaredField("gameStarted");
        field.setAccessible(true);
        boolean started = field.getBoolean(null);
        try {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().add(new Mode(ChallengeType.BINGO));
            ChallengeManager.markGameStarted();
            GameEntryPolicy finished = new GameEntryPolicy() {
                public int priority() { return 1; }
                public boolean finished() { return true; }
                public Admission prepare(UUID id, EntryReason reason) { return new Admission(false, null, null); }
                public void admitted(UUID id, EntryReason reason) {}
                public void left(UUID id, LeaveReason reason) {}
                public List<Location> spectatorTargets() { return List.of(); }
            };
            GameEntryService service = new GameEntryService(finished);
            service.beginRound(UUID.randomUUID(), Set.of(UUID.randomUUID()));
            assertEquals(List.of(), new CurrentActiveGameProvider(service).activeGames());
        } finally {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().addAll(previous);
            field.setBoolean(null, started);
        }
    }
    private record Mode(ChallengeType type) implements Challenge {
        public ChallengeType getType() { return type; }
        public List<KatzeCommand> getCommands() { return List.of(); }
        public List<Listener> getListeners() { return List.of(); }
        public List<Runnable> getTasks() { return List.of(); }
    }
}
