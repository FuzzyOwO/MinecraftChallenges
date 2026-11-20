package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.results.BattleResultsStore;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyResultsTest {
    @Test void playerCollectionsRemainSeparateFromTeamTotalsAfterRestart(@TempDir Path directory) throws Exception {
        var match = new MobArmyMatch();
        var team = side("team-1", 1200, false);
        UUID first = team.members.keySet().iterator().next(), second = UUID.randomUUID(), empty = UUID.randomUUID();
        team.members.put(second, "Second");
        team.members.put(empty, "Empty");
        for (UUID collector : List.of(first, first, second)) {
            var mob = new MobArmyMatch.CapturedMob();
            mob.type = "ZOMBIE";
            mob.collector = collector;
            team.mobs.add(mob);
        }
        var golem = new MobArmyMatch.CapturedMob();
        golem.type = "IRON_GOLEM";
        golem.collector = second;
        team.mobs.add(golem);
        match.sides = List.of(team, side("team-2", 1400, false));
        Path state = directory.resolve("match.json");
        JsonFiles.write(state, match);
        var restored = JsonFiles.read(state, MobArmyMatch.class);
        var store = new BattleResultsStore(directory.resolve("results"));
        store.save(MobArmyResults.capture(restored));
        var saved = store.findForPlayer(match.id, first).orElseThrow();
        var firstResult = saved.players().stream().filter(player -> player.id().equals(first)).findFirst().orElseThrow();
        var secondResult = saved.players().stream().filter(player -> player.id().equals(second)).findFirst().orElseThrow();
        var emptyResult = saved.players().stream().filter(player -> player.id().equals(empty)).findFirst().orElseThrow();
        assertTrue(firstResult.personalCollection());
        assertEquals(1, firstResult.objectives().size());
        assertEquals("ZOMBIE_SPAWN_EGG", firstResult.objectives().getFirst().icon());
        assertEquals(2, firstResult.objectives().getFirst().count());
        assertEquals(2, firstResult.statistics().get(2).value());
        assertEquals(2, secondResult.objectives().size());
        assertEquals(2, secondResult.statistics().get(2).value());
        assertTrue(emptyResult.objectives().isEmpty());
        assertEquals(0, emptyResult.statistics().get(2).value());
        assertEquals(4, saved.teams().getFirst().objectives().stream().mapToInt(SavedBattle.Objective::count).sum());
    }

    @Test void archivesOneEntryPerTypeWithCombinedCountsIncludingVariants(@TempDir Path directory) throws Exception {
        var match = new MobArmyMatch();
        var team = side("team-1", 1200, false);
        for (String type : List.of("ZOMBIE", "ZOMBIE", "IRON_GOLEM", "ZOMBIE")) {
            var mob = new MobArmyMatch.CapturedMob();
            mob.type = type;
            mob.variant = UUID.randomUUID().toString();
            team.mobs.add(mob);
        }
        match.sides = List.of(team, side("team-2", 1400, false));
        var battle = MobArmyResults.capture(match);
        var store = new BattleResultsStore(directory);
        store.save(battle);
        var saved = store.findForPlayer(match.id, team.members.keySet().iterator().next()).orElseThrow();
        var mobs = saved.teams().getFirst().objectives();
        assertEquals(2, mobs.size());
        assertEquals("IRON_GOLEM_SPAWN_EGG", mobs.getFirst().icon());
        assertEquals(1, mobs.getFirst().count());
        assertEquals("ZOMBIE_SPAWN_EGG", mobs.getLast().icon());
        assertEquals(3, mobs.getLast().count());
        assertEquals(mobs, saved.players().getFirst().objectives());
        assertEquals(4, saved.players().getFirst().statistics().get(2).value());
    }

    @Test void archivesParticipantsTimesTeamsAndCollectedMobsAfterRestart(@TempDir Path directory) throws Exception {
        var match = new MobArmyMatch();
        match.startedAt = 1000;
        match.endedAt = 121000;
        var team = side("team-4", 1250, false);
        team.members.put(UUID.randomUUID(), "Teammate");
        var mob = new MobArmyMatch.CapturedMob();
        mob.type = "IRON_GOLEM";
        team.mobs.add(mob);
        var solo = side(UUID.randomUUID().toString(), 1800, false);
        var forfeited = side(UUID.randomUUID().toString(), 0, true);
        match.sides = List.of(solo, forfeited, team);
        var battle = MobArmyResults.capture(match);
        var store = new BattleResultsStore(directory);
        store.save(battle);
        var restored = new BattleResultsStore(directory).forPlayer(team.members.keySet().iterator().next()).getFirst();
        assertEquals(battle, restored);
        assertEquals(SavedBattle.Game.MOB_ARMY, restored.game());
        assertEquals(120, restored.durationSeconds());
        assertEquals(4, restored.players().size());
        assertEquals(4, restored.teams().getFirst().id());
        assertEquals(1, restored.teams().getFirst().points());
        assertEquals(1, restored.teams().getFirst().objectives().size());
        assertEquals(1250, restored.players().getFirst().statistics().get(1).value());
        assertEquals(1, restored.players().getLast().statistics().getLast().value());
        assertTrue(store.forPlayer(UUID.randomUUID()).isEmpty());
    }

    private MobArmyMatch.Side side(String id, long ticks, boolean forfeited) {
        var side = new MobArmyMatch.Side();
        side.id = id;
        side.members.put(UUID.randomUUID(), id);
        side.elapsedTicks = ticks;
        side.finished = true;
        side.forfeited = forfeited;
        return side;
    }
}
