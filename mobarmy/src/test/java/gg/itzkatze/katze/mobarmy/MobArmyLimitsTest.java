package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyLimitsTest {

    private static final MobLimitTable LIMITS = MobLimitTable.parse(List.of("ZOMBIE: 500", "CREAKING: 0"));

    @Test void capsComeFromTheTableWhileLimitsAreOn() {
        assertEquals(500, MobArmyGame.limit(LIMITS, true, EntityType.ZOMBIE));
        assertEquals(MobLimitTable.UNLIMITED, MobArmyGame.limit(LIMITS, true, EntityType.COW));
    }

    @Test void turningLimitsOffUncapsMobsButKeepsDisabledMobsDisabled() {
        assertEquals(MobLimitTable.UNLIMITED, MobArmyGame.limit(LIMITS, false, EntityType.ZOMBIE));
        assertEquals(0, MobArmyGame.limit(LIMITS, false, EntityType.CREAKING));
    }
}
