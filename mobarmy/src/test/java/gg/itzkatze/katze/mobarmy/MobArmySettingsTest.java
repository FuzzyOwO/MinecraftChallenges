package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobArmySettingsTest {

    @Test void mobLimitRoundTripsThroughSerialization() {
        var limit = new MobArmySettings.MobLimit(25);
        assertEquals("25", limit.serialize());
        assertInstanceOf(MobArmySettings.MobLimit.class, limit.parse("7"));
        assertEquals(7, ((MobArmySettings.MobLimit) limit.parse("7")).amount());
        assertNull(limit.parse("abc"));
        assertNull(limit.parse("0"));
        assertNull(limit.parse("1001"));
    }

    @Test void mobLimitRejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> new MobArmySettings.MobLimit(0));
        assertThrows(IllegalArgumentException.class, () -> new MobArmySettings.MobLimit(1001));
    }

    @Test void defaultMobLimitIsWithinEditableRange() {
        var def = (MobArmySettings.MobLimit) MobArmySettings.MOB_LIMIT.getDefaultState();
        assertTrue(def.amount() >= 1 && def.amount() <= 1000);
    }
}
