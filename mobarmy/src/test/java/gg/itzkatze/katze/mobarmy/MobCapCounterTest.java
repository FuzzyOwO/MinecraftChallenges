package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobCapCounterTest {

    @Test void countsPerTypeAndIncrementsSequentially() {
        var counter = new MobCapCounter();
        assertEquals(1, counter.recordKill("team-a", "ZOMBIE"));
        assertEquals(2, counter.recordKill("team-a", "ZOMBIE"));
        assertEquals(1, counter.recordKill("team-a", "CREEPER"));
    }

    @Test void sidesAreCountedSeparately() {
        var counter = new MobCapCounter();
        counter.recordKill("team-a", "ZOMBIE");
        assertEquals(1, counter.recordKill("team-b", "ZOMBIE"));
    }

    @Test void resetClearsCounts() {
        var counter = new MobCapCounter();
        counter.recordKill("team-a", "ZOMBIE");
        counter.recordKill("team-a", "ZOMBIE");
        counter.reset();
        assertEquals(1, counter.recordKill("team-a", "ZOMBIE"));
    }
}
