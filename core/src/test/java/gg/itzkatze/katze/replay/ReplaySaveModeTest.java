package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplaySaveModeTest {
    @Test void phasedGamesCycleThroughFullBattleAndOff() {
        var full = ReplaySaveMode.withBattlePhase();
        var battle = full.next();
        var off = battle.next();
        assertEquals(ReplaySaveMode.Scope.ALL, full.scope());
        assertEquals(ReplaySaveMode.Scope.BATTLE, battle.scope());
        assertEquals(ReplaySaveMode.Scope.OFF, off.scope());
        assertEquals(full, off.next());
        assertTrue(full.records(false));
        assertTrue(full.records(true));
        assertFalse(battle.records(false));
        assertTrue(battle.records(true));
        assertFalse(off.records(false));
        assertFalse(off.records(true));
    }

    @Test void gamesWithoutASeparateBattlePhaseOnlyOfferFullAndOff() {
        var full = ReplaySaveMode.fullGame();
        assertEquals(ReplaySaveMode.Scope.OFF, full.next().scope());
        assertEquals(full, full.next().next());
        assertNull(full.parse("BATTLE"));
    }

    @Test void savedStatesRestoreTheirSupportedScopeAndRejectInvalidValues() {
        var phased = ReplaySaveMode.withBattlePhase();
        for (int i = 0; i < 3; i++) {
            var selected = i == 0 ? phased : i == 1 ? phased.next() : phased.next().next();
            assertEquals(selected, phased.parse(selected.serialize()));
        }
        assertEquals(phased.next(), phased.parse(" battle "));
        assertNull(phased.parse("unknown"));
        assertEquals(ReplaySaveMode.Scope.OFF, ReplaySaveMode.from(null).scope());
    }
}
