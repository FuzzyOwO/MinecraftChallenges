package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import lombok.Getter;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;

@Getter
public enum ForceBattleType {

    FORCE_ITEM("Item", "Item", ForceBattleSettings.FORCE_ITEM),
    FORCE_MOB("Mob", "Mob", ForceBattleSettings.FORCE_MOB),
    FORCE_BIOME("Biome", "Biom", ForceBattleSettings.FORCE_BIOME),
    FORCE_ADVANCEMENT("Advancement", "Advancement", ForceBattleSettings.FORCE_ADVANCEMENT),
    FORCE_HEIGHT("Height", "Höhe", ForceBattleSettings.FORCE_HEIGHT),
    FORCE_COORDS("Coords", "Koordinaten", ForceBattleSettings.FORCE_COORDS),
    FORCE_STRUCTURE("Structure", "Struktur", ForceBattleSettings.FORCE_STRUCTURE),
    FORCE_DEATH("Death", "Tod", ForceBattleSettings.FORCE_DEATH),
    ;

    private final String englishPrefix;
    private final String germanPrefix;
    private final ForceBattleSettings linkedSetting;

    ForceBattleType(String englishPrefix, String germanPrefix, ForceBattleSettings linkedSetting) {
        this.englishPrefix = englishPrefix;
        this.germanPrefix = germanPrefix;
        this.linkedSetting = linkedSetting;
    }

    public String getPrefix() {
        return englishPrefix;
    }

    public SettingStateType getSettingState() {
        Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
        if (forceBattle != null && forceBattle.getSettingsManager() != null) {
            ChallengeSettingsManager manager = forceBattle.getSettingsManager();
            return manager.getState(linkedSetting);
        }
        return PlayerTeamBothState.OFF;
    }
}
