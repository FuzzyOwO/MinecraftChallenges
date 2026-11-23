package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobLimitTableTest {

    private static MobLimitTable table(String... lines) {return MobLimitTable.parse(List.of(lines));}

    @Test void readsCapsAndTreatsUnlistedTypesAsUncapped() {
        var limits = table("ZOMBIE: 500", "IRON_GOLEM: 50");
        assertEquals(500, limits.cap(EntityType.ZOMBIE));
        assertEquals(50, limits.cap(EntityType.IRON_GOLEM));
        assertEquals(MobLimitTable.UNLIMITED, limits.cap(EntityType.COW));
    }

    @Test void zeroDisablesAMobType() {
        assertEquals(0, table("ENDER_DRAGON: 0").cap(EntityType.ENDER_DRAGON));
    }

    @Test void ignoresCommentsBlankLinesAndTrailingNotes() {
        var limits = table("# Animals & Misc", "", "  COW: 500  # farm mobs", "\t");
        assertEquals(500, limits.cap(EntityType.COW));
        assertEquals(1, limits.size());
    }

    @Test void acceptsAnyLetterCaseAndKeepsTheLastOfDuplicateKeys() {
        var limits = table("cow: 100", "Cow: 500");
        assertEquals(500, limits.cap(EntityType.COW));
        assertEquals(1, limits.size());
    }

    @Test void skipsLinesWithUnknownMobsOrBrokenCaps() {
        var limits = table("NOT_A_MOB: 10", "COW", "COW: many", "COW: -5", "SHEEP: 500");
        assertEquals(MobLimitTable.UNLIMITED, limits.cap(EntityType.COW));
        assertEquals(500, limits.cap(EntityType.SHEEP));
        assertEquals(1, limits.size());
    }

    @Test void toleratesAWindowsByteOrderMark() {
        assertEquals(500, table("\uFEFFCOW: 500").cap(EntityType.COW));
    }

    @Test void mergeAddsMissingDefaultsInOrder() {
        assertEquals(List.of("AXOLOTL: 200", "COW: 500"),
                MobLimitTable.missingDefaultLines(List.of("AXOLOTL: 200", "COW: 500"), List.of("ZOMBIE: 5")));
    }

    @Test void mergeTreatsLowerCasedAndCommentedLinesAsPresent() {
        assertEquals(List.of(), MobLimitTable.missingDefaultLines(
                List.of("ZOMBIE: 9", "COW: 500"), List.of("zombie: 5", "# COW: 500")));
    }

    @Test void mergeKeepsSimilarKeyPrefixesApart() {
        assertEquals(List.of("ZOMBIE: 9"),
                MobLimitTable.missingDefaultLines(List.of("ZOMBIE: 9"), List.of("ZOMBIE_HORSE: 5")));
        assertEquals(List.of("ZOMBIE_HORSE: 5"),
                MobLimitTable.missingDefaultLines(List.of("ZOMBIE_HORSE: 5"), List.of("ZOMBIE: 9")));
    }

    @Test void mergeSeesKeysAheadOfTrailingCommentsAndSkipsNonEntryDefaults() {
        assertEquals(List.of(), MobLimitTable.missingDefaultLines(
                List.of("# Boss", "", "not an entry", "SHEEP: 500 # farm"), List.of("anything SHEEP: 500 # farm")));
    }
}
