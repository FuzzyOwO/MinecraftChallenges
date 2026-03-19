package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;

/**
 * Unified manager for all MinecraftChallenges settings.
 * This provides centralized access to both global settings and challenge-specific settings.
 * - Global settings: Accessible via GlobalSetting enum
 * - All settings can be queried/modified through unified methods
 */
public class SettingsManager {

    private static final ChallengeSettingsManager GLOBAL_SETTINGS_MANAGER =
            new ChallengeSettingsManager(null, GlobalSetting.class, SettingsManager::onGlobalSettingChanged);

    private SettingsManager() {}

    /**
     * Callback when a global setting changes.
     */
    private static void onGlobalSettingChanged(gg.itzkatze.mcc.challenges.Challenge challenge, Setting setting,
                                               SettingStateType oldState, SettingStateType newState) {
        // Global settings change handler can be extended here
    }

    /**
     * Gets the state of a global setting.
     * @param setting The global setting to query
     * @return The current state
     */
    public static SettingStateType getState(Setting setting) {
        return GLOBAL_SETTINGS_MANAGER.getState(setting);
    }

    /**
     * Sets the state of a global setting.
     * @param setting The global setting to update
     * @param state The new state
     */
    public static void setState(Setting setting, SettingStateType state) {
        if (state == null) {
            state = setting.getDefaultState();
        }
        if (state != null) {
            GLOBAL_SETTINGS_MANAGER.setState(setting, state);
        }
    }

    /**
     * Checks if a global setting is enabled (not OFF).
     * @param setting The setting to check
     * @return True if the setting is not OFF
     */
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

    /**
     * Callback when a global setting changes.
     */
    private static void onGlobalSettingChanged(Object challenge, Setting setting, PlayerTeamBothState oldState, PlayerTeamBothState newState) {
        if (!(setting instanceof GlobalSetting)) {
            return;
        }

        // Update UI when global settings change
        MccPlayer.PLAYERS.forEach(player -> player.updateObjective(false, false));
        MccTeam.teams.forEach(team -> team.updateObjective(null, false, false));
    }

    /**
     * Finds the settings manager that owns a particular setting.
     * Iterates through all registered challenges to find which one has this setting.
     * @param setting The setting to find the owner for
     * @return The manager that owns this setting, or null if not found
     */
    private static ChallengeSettingsManager findSettingsManagerFor(Object setting) {
        // Try global settings first (most common case)
        if (setting instanceof GlobalSetting) {
            return GLOBAL_SETTINGS_MANAGER;
        }

        // Search through all registered challenges for the setting
        for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
            ChallengeSettingsManager challengeManager = challenge.getSettingsManager();
            if (challengeManager == null) continue;

            try {

                // Check if this manager has the setting
                for (Setting enumValue : challengeManager.getAllSettings()) {
                    if (enumValue == setting || enumValue.equals(setting)) {
                        return challengeManager;
                    }
                }
            } catch (Exception e) {
                // Continue searching other challenges
            }
        }
        return null;
    }

    /**
     * Unified method to check if any setting is enabled (global or challenge-specific).
     * Automatically discovers which challenge owns the setting - no hardcoding needed.
     * @param setting The setting to check (can be GlobalSetting, ForceBattleSettings, or any challenge setting)
     * @return True if the setting is not OFF
     */
    public static boolean isEnabled(Object setting) {
        if (setting instanceof GlobalSetting globalSetting) {
            return GLOBAL_SETTINGS_MANAGER.isEnabled(globalSetting);
        }

        // For any other setting, find which challenge owns it
        ChallengeSettingsManager manager = findSettingsManagerFor(setting);
        if (manager != null && setting instanceof Setting s) {
            try {
                return manager.isEnabled((Enum<?> & Setting) setting);
            } catch (Exception e) {
                // Fall through to return false
            }
        }
        return false;
    }

    /**
     * Unified method to get the state of any setting.
     * Automatically discovers which challenge owns the setting - no hardcoding needed.
     * @param setting The setting to check (can be GlobalSetting, ForceBattleSettings, or any challenge setting)
     * @return The current state, or the first enum constant (OFF-like state) if setting/challenge not found
     */
    public static SettingStateType getSettingState(Object setting) {
        if (setting instanceof GlobalSetting globalSetting) {
            return GLOBAL_SETTINGS_MANAGER.getState(globalSetting);
        }

        // For any other setting, find which challenge owns it
        ChallengeSettingsManager manager = findSettingsManagerFor(setting);
        if (manager != null && setting instanceof Setting s) {
            try {
                return manager.getState((Enum<?> & Setting) setting);
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        return null;
    }

    /**
     * Unified method to check if a setting is multi-state (OFF -> PLAYER -> TEAM -> BOTH).
     * @param setting The setting to check (GlobalSetting or ForceBattleSettings)
     * @return True if the setting supports multiple states
     */
    public static boolean isMultiState(Object setting) {
        if (setting instanceof Setting s) {
            return s.shouldUpdateOnChange();
        }
        return false;
    }
}
