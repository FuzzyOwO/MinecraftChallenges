package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for handling challenge-specific settings.
 * Each challenge can have its own set of configurable settings managed by this system.
 * Supports any SettingStateType: SettingState (multi-state), OnOff (binary), or custom types.
 */
public class ChallengeSettingsManager {

    private final Challenge challenge;
    private final Class<?> settingClass;
    private final Map<Setting, SettingStateType> states;
    private final ChallengeSettingsChangeListener changeListener;

    /**
     * Creates a new settings manager for a challenge.
     *
     * @param challenge The challenge this manager is for
     * @param settingClass The Setting enum class for this challenge
     * @param changeListener Optional listener for setting changes
     */
    public ChallengeSettingsManager(Challenge challenge, @NotNull Class<?> settingClass,
                                    @Nullable ChallengeSettingsChangeListener changeListener) {
        this.challenge = challenge;
        this.settingClass = settingClass;
        this.states = new HashMap<>();
        this.changeListener = changeListener;

        // Initialize all settings to their default state
        for (Object constant : settingClass.getEnumConstants()) {
            Setting setting = (Setting) constant;
            SettingStateType defaultState = setting.getDefaultState();
            if (defaultState != null) {
                states.put(setting, defaultState);
            }
        }
    }

    /**
     * Creates a new settings manager for a challenge without a change listener.
     *
     * @param challenge The challenge this manager is for
     * @param settingClass The Setting enum class for this challenge
     */
    public ChallengeSettingsManager(@NotNull Challenge challenge, @NotNull Class<?> settingClass) {
        this(challenge, settingClass, null);
    }

    /**
     * Gets the challenge this manager manages settings for.
     *
     * @return The challenge
     */
    public @NotNull Challenge getChallenge() {
        return challenge;
    }

    /**
     * Gets the current state of a setting.
     *
     * @param setting The setting to query
     * @return The current state, or the first enum constant (OFF-like state) if not found
     */
    public @NotNull SettingStateType getState(@NotNull Setting setting) {
        return states.getOrDefault(setting, null);
    }

    /**
     * Sets the state of a setting.
     *
     * @param setting The setting to update
     * @param state The new state
     */
    public void setState(@NotNull Setting setting, @NotNull SettingStateType state) {
        SettingStateType oldState = states.put(setting, state);

        if (setting.shouldUpdateOnChange() && changeListener != null) {
            changeListener.onSettingChanged(challenge, setting, oldState, state);
        }
    }

    /**
     * Checks if a setting is enabled (not the first/OFF state).
     * Works with any state type: SettingState (checks != OFF), OnOff (checks == ON), etc.
     *
     * @param setting The setting to check
     * @return True if the setting is not in its "off" state (first enum constant)
     */
    public boolean isEnabled(@NotNull Setting setting) {
        SettingStateType state = getState(setting);

        // Get the first enum constant as the "off" state
        Class<? extends SettingStateType> stateType = state.getClass();
        Object[] stateConstants = stateType.getEnumConstants();
        if (stateConstants == null || stateConstants.length == 0) {
            return true;
        }
        return state != stateConstants[0];
    }

    /**
     * Gets all settings for this manager.
     *
     * @return Array of all settings
     */
    public Setting[] getAllSettings() {
        Object[] constants = settingClass.getEnumConstants();
        Setting[] settings = new Setting[constants.length];
        for (int i = 0; i < constants.length; i++) {
            settings[i] = (Setting) constants[i];
        }
        return settings;
    }

    /**
     * Resets all settings to their default state.
     */
    public void resetAll() {
        for (Setting setting : getAllSettings()) {
            SettingStateType defaultState = setting.getDefaultState();
            if (defaultState != null) {
                states.put(setting, defaultState);
            }
        }
    }

    /**
     * Listener interface for challenge setting changes.
     */
    public interface ChallengeSettingsChangeListener {
        /**
         * Called when a challenge setting changes.
         *
         * @param challenge The challenge
         * @param setting The setting that changed
         * @param oldState The old state
         * @param newState The new state
         */
        void onSettingChanged(Challenge challenge, Setting setting, SettingStateType oldState, SettingStateType newState);
    }
}
