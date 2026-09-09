package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ChallengeSettingsManager {

    @Getter
    private final Challenge challenge;
    private final Class<?> settingClass;
    private final Map<Setting, SettingStateType> states;
    private final ChallengeSettingsChangeListener changeListener;

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

    public ChallengeSettingsManager(@NotNull Challenge challenge, @NotNull Class<?> settingClass) {
        this(challenge, settingClass, null);
    }

    public @NotNull SettingStateType getState(@NotNull Setting setting) {
        return states.getOrDefault(setting, null);
    }

    public void setState(@NotNull Setting setting, @NotNull SettingStateType state) {
        SettingStateType oldState = states.put(setting, state);

        if (setting.shouldUpdateOnChange() && changeListener != null) {
            changeListener.onSettingChanged(challenge, setting, oldState, state);
        }
    }

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

    public Setting[] getAllSettings() {
        Object[] constants = settingClass.getEnumConstants();
        Setting[] settings = new Setting[constants.length];
        for (int i = 0; i < constants.length; i++) {
            settings[i] = (Setting) constants[i];
        }
        return settings;
    }

    public void resetAll() {
        for (Setting setting : getAllSettings()) {
            SettingStateType defaultState = setting.getDefaultState();
            if (defaultState != null) {
                states.put(setting, defaultState);
            }
        }
    }

    public interface ChallengeSettingsChangeListener {
        void onSettingChanged(Challenge challenge, Setting setting, SettingStateType oldState, SettingStateType newState);
    }
}
