package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.results.SavedBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplayScoreboardTest {
    private final UUID player = UUID.randomUUID(), world = UUID.randomUUID();

    @Test void everyModeUsesItsRecordedInformationInsteadOfOtherChallengesOrFinalResults() {
        for (SavedBattle.Game game : SavedBattle.Game.values()) {
            Component objective = Component.text(game.displayName() + " recorded progress");
            var state = state(Map.of(game.name(), List.of(json(objective)), "UNRELATED", List.of(json(Component.text("Wrong challenge")))));
            var lines = ReplayScoreboard.lines(battle(game), player, state, 30, 60, false, 2);
            assertTrue(Arrays.asList(lines).contains(objective));
            assertFalse(Arrays.asList(lines).contains(Component.text("Wrong challenge")));
            assertFalse(hasKey(lines, "katze.replay.sidebar.final"));
            assertTrue(hasKey(lines, "katze.replay.sidebar.playing"));
            assertTrue(hasKey(lines, "katze.replay.sidebar.player_team"));
        }
    }

    @Test void replaySidebarFollowsRecordedProgressWhenSeekingBackwards() {
        var first = state(Map.of("MOB_ARMY", List.of(json(Component.text("Wave 1 · 10 mobs")))));
        var next = state(Map.of("MOB_ARMY", List.of(json(Component.text("Wave 2 · 5 mobs")))));
        var timeline = new ReplayTimeline(List.of(frame(0, first), frame(20, next)), 40);
        timeline.moveTo(20, (block, reverse) -> fail());
        assertTrue(Arrays.asList(ReplayScoreboard.lines(battle(SavedBattle.Game.MOB_ARMY), player, timeline.player(player), 1, 2, false, 1))
                .contains(Component.text("Wave 2 · 5 mobs")));
        timeline.moveTo(0, (block, reverse) -> fail());
        var lines = ReplayScoreboard.lines(battle(SavedBattle.Game.MOB_ARMY), player, timeline.player(player), 0, 2, true, 0.5);
        assertTrue(Arrays.asList(lines).contains(Component.text("Wave 1 · 10 mobs")));
        assertTrue(hasKey(lines, "katze.replay.sidebar.paused"));
    }

    @Test void oldRecordingsClearlyLabelFinalResultsAndNeverInventLiveProgress() {
        var lines = ReplayScoreboard.lines(battle(SavedBattle.Game.FORCE_BATTLE), player, null, 5, 60, false, 1);
        assertTrue(hasKey(lines, "katze.replay.sidebar.final"));
        assertTrue(hasKey(lines, "katze.results.points"));
        assertFalse(hasKey(lines, "katze.replay.sidebar.wave"));
    }

    @Test void limitsTheSidebarToFifteenLinesWhileKeepingReplayControlsAndImportantInformation() {
        var state = state(Map.of("ALL_ITEMS", IntStream.range(0, 30).mapToObj(index -> json(Component.text("Progress " + index))).toList()));
        var lines = ReplayScoreboard.lines(battle(SavedBattle.Game.ALL_ITEMS), player, state, 5, 60, false, 1);
        assertEquals(15, lines.length);
        assertTrue(hasKey(lines, "katze.replay.sidebar.progress"));
        assertTrue(Arrays.asList(lines).contains(Component.text("Progress 0")));
        assertFalse(Arrays.asList(lines).contains(Component.text("Progress 29")));
    }

    private SavedBattle battle(SavedBattle.Game game) {
        return new SavedBattle(UUID.randomUUID(), 0, 60000, 60,
                List.of(new SavedBattle.Player(player, "Recorded player", 2, 9, List.of())), List.of(), game);
    }

    private ReplayFrame.PlayerState state(Map<String, List<String>> scoreboards) {
        return new ReplayFrame.PlayerState(List.of(), null, 2, 12, 20, 18, 4, "game", List.of(), scoreboards);
    }

    private ReplayFrame frame(int tick, ReplayFrame.PlayerState state) {
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Recorded player", null, null, null,
                0, 64, 0, 0, 0, "STANDING", false, false, List.of(), null, false, 0, null, state);
        return new ReplayFrame(tick, List.of(actor), List.of(), List.of());
    }

    private String json(Component component) { return GsonComponentSerializer.gson().serialize(component); }

    private boolean hasKey(Component[] lines, String key) {
        return Arrays.stream(lines).anyMatch(line -> line instanceof TranslatableComponent translated && translated.key().equals(key));
    }
}
