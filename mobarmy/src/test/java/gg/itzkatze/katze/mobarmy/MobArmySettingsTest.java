package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.configuration.states.OnOffState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobArmySettingsTest {

    @Test void mobLimitsAreOnByDefault() {
        assertEquals(OnOffState.ON, MobArmySettings.MOB_LIMIT.getDefaultState());
    }

    @Test void aStoredNumericMobLimitFromAnEarlierVersionIsRejected() {
        assertFalse(new MobArmy().getSettingsManager().importState(MobArmySettings.MOB_LIMIT, "100"));
    }

    @Test void mobLimitsStoreAsAnOnOffState() {
        var manager = new MobArmy().getSettingsManager();
        assertTrue(manager.importState(MobArmySettings.MOB_LIMIT, OnOffState.OFF.name()));
        assertFalse(manager.isEnabled(MobArmySettings.MOB_LIMIT));
        assertTrue(manager.exportStates().containsKey(MobArmySettings.MOB_LIMIT.getId()));
    }
}
