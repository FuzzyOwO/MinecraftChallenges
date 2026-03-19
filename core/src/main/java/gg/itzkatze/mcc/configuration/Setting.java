package gg.itzkatze.mcc.configuration;

import gg.itzkatze.mcc.configuration.states.SettingStateType;

/**
 * Interface for a configurable setting.
 * Implementations should be enums that define all settings for a particular challenge or system.
 */
public interface Setting {

    /**
     * Gets the unique identifier for this setting.
     * @return The setting ID
     */
    String getId();

    /**
     * Gets the display name for this setting.
     * @return The display name
     */
    String getDisplayName();

    /**
     * Whether changes to this setting should trigger an update notification.
     * @return True if updates should be triggered
     */
    boolean shouldUpdateOnChange();

    /**
     * Gets the default state for this setting.
     * This is used when initializing or resetting settings.
     * Must be an instance of the type returned by getStateType().
     *
     * @return The default state
     */
    SettingStateType getDefaultState();
}
