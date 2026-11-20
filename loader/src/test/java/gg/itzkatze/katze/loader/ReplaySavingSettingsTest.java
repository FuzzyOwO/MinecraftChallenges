package gg.itzkatze.katze.loader;

import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.replay.ReplaySaveMode;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.randomizer.RandomizerSettings;
import gg.itzkatze.katze.itemrain.ItemRainSettings;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplaySavingSettingsTest {
    @Test void everyMainModeHasAnIndependentPersistedReplaySetting() {
        List<ChallengeType> modes = List.of(ChallengeType.FORCE_BATTLE, ChallengeType.BINGO, ChallengeType.MOB_ARMY,
                ChallengeType.ALL_ITEMS, ChallengeType.DEATH_SWAP);
        for (ChallengeType type : modes) {
            var challenge = ChallengeRegistry.getChallenge(type);
            assertNotNull(challenge, type.name());
            var manager = challenge.getSettingsManager();
            assertNotNull(manager, type.name());
            var setting = Arrays.stream(manager.getAllSettings()).filter(option -> option.getId().equals("REPLAY_SAVING")).findFirst().orElseThrow();
            var original = manager.exportStates();
            try {
                var full = assertInstanceOf(ReplaySaveMode.class, manager.getState(setting));
                assertEquals(ReplaySaveMode.Scope.ALL, full.scope());
                var selected = full.next();
                manager.setState(setting, selected);
                assertEquals(type == ChallengeType.MOB_ARMY ? ReplaySaveMode.Scope.BATTLE : ReplaySaveMode.Scope.OFF, selected.scope());
                var saved = manager.exportStates();
                manager.resetAll();
                manager.importStates(saved);
                assertEquals(selected, ReplaySaveMode.from(challenge));
                for (ChallengeType other : modes) {
                    if (other != type) assertEquals(ReplaySaveMode.Scope.ALL, ReplaySaveMode.from(ChallengeRegistry.getChallenge(other)).scope());
                }
            } finally { manager.importStates(original); }
        }
    }

    @Test void gameplayModifiersDoNotOfferReplaySettings() {
        for (Class<?> settings : List.of(RandomizerSettings.RandomizerSetting.class, ItemRainSettings.ItemRainSetting.class)) {
            for (Object value : settings.getEnumConstants()) {
                Setting setting = (Setting) value;
                assertNotEquals("REPLAY_SAVING", setting.getId());
                assertFalse(setting.getDefaultState() instanceof ReplaySaveMode);
            }
        }
    }
}
