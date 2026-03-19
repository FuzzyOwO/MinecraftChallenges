package gg.itzkatze.mcc.challenges;

import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import org.jetbrains.annotations.Nullable;

public interface ChallengeSettings {

    default MccGUI getSettingsGUI() {
        return null;
    }

    default boolean hasSettings() {
        return getSettingsGUI() != null || getSettingsManager() != null;
    }

    @Nullable default ChallengeSettingsManager getSettingsManager() {
        return null;
    }
}
