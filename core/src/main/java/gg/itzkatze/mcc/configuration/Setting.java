package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.configuration.states.SettingStateType;

public interface Setting {

    String getId();

    String getDisplayName();

    boolean shouldUpdateOnChange();

    SettingStateType getDefaultState();
}
