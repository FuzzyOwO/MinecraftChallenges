package gg.itzkatze.katze.results;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SavedMobArmyResultsTest {
    @Test
    void combinesOldIndividualEntriesWithNewCountedEntriesByMobType() {
        var old = new Gson().fromJson("""
                {"name":"zombie","type":"collected","icon":"ZOMBIE_SPAWN_EGG","finishedBy":"","remainingSeconds":-1,"finished":true,"skipped":false}
                """, SavedBattle.Objective.class);
        var grouped = new SavedBattle.Objective("zombie", "collected", "ZOMBIE_SPAWN_EGG", "", -1, true, false, 12);
        var skeleton = new SavedBattle.Objective("skeleton", "collected", "SKELETON_SPAWN_EGG", "", -1, true, false);
        var result = SavedMobArmyResults.group(List.of(old, skeleton, grouped, old));
        assertEquals(1, old.count());
        assertEquals(2, result.size());
        assertEquals("SKELETON_SPAWN_EGG", result.getFirst().icon());
        assertEquals(1, result.getFirst().count());
        assertEquals(14, result.getLast().count());
    }
}
