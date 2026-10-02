package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;

public class SettingsManager {

    private static final ChallengeSettingsManager GLOBAL_SETTINGS_MANAGER =
            new ChallengeSettingsManager(null, GlobalSetting.class, SettingsManager::onGlobalSettingChanged);

    private SettingsManager() {}

    private static void onGlobalSettingChanged(Challenge challenge, Setting setting, SettingStateType oldState, SettingStateType newState) {
    }

    public static ChallengeSettingsManager getGlobalSettingsManager() {
        return GLOBAL_SETTINGS_MANAGER;
    }

    public static SettingStateType getState(Setting setting) {
        return GLOBAL_SETTINGS_MANAGER.getState(setting);
    }

    public static void setState(Setting setting, SettingStateType state) {
        if (state == null) {
            state = setting.getDefaultState();
        }
        if (state != null) {
            GLOBAL_SETTINGS_MANAGER.setState(setting, state);
        }
    }

    public static boolean isEnabled(Setting setting) {
        if (setting instanceof GlobalSetting) {
            return GLOBAL_SETTINGS_MANAGER.isEnabled(setting);
        }
        ChallengeSettingsManager manager = findSettingsManagerFor(setting);
        if (manager != null) {
            return manager.isEnabled(setting);
        }
        return false;
    }

    private static void onGlobalSettingChanged(Object challenge, Setting setting, PlayerTeamBothState oldState, PlayerTeamBothState newState) {
        if (!(setting instanceof GlobalSetting)) {
            return;
        }

        MccPlayer.PLAYERS.forEach(player -> player.updateObjective(false, false));
        MccTeam.teams.forEach(team -> team.updateObjective(null, false, false));
    }

    private static ChallengeSettingsManager findSettingsManagerFor(Object setting) {
        if (setting instanceof GlobalSetting) {
            return GLOBAL_SETTINGS_MANAGER;
        }

        for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
            ChallengeSettingsManager challengeManager = challenge.getSettingsManager();
            if (challengeManager == null) continue;

            try {
                for (Setting enumValue : challengeManager.getAllSettings()) {
                    if (enumValue == setting || enumValue.equals(setting)) {
                        return challengeManager;
                    }
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static boolean isEnabled(Object setting) {
        if (setting instanceof GlobalSetting globalSetting) {
            return GLOBAL_SETTINGS_MANAGER.isEnabled(globalSetting);
        }

        ChallengeSettingsManager manager = findSettingsManagerFor(setting);
        if (manager != null && setting instanceof Setting s) {
            try {
                return manager.isEnabled((Enum<?> & Setting) setting);
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static SettingStateType getSettingState(Object setting) {
        if (setting instanceof GlobalSetting globalSetting) {
            return GLOBAL_SETTINGS_MANAGER.getState(globalSetting);
        }

        ChallengeSettingsManager manager = findSettingsManagerFor(setting);
        if (manager != null && setting instanceof Setting s) {
            try {
                return manager.getState((Enum<?> & Setting) setting);
            } catch (Exception e) {
            }
        }
        return null;
    }
}
