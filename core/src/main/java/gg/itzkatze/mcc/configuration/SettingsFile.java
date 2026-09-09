package gg.itzkatze.mcc.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeRegistry;
import gg.itzkatze.mcc.configuration.states.ConfigurableSettingState;
import gg.itzkatze.mcc.configuration.states.SettingStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class SettingsFile {

    private static final Logger logger = LoggerFactory.getLogger("ForceBattle/" + SettingsFile.class.getSimpleName());
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static void loadSettingsFromFile(File jsonFile) throws IOException {
        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject settingsObject = JsonParser.parseReader(reader).getAsJsonObject();

            for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
                loadChallengeSettings(challenge, settingsObject);
            }
        }
    }

    private static void loadChallengeSettings(Challenge challenge, JsonObject settingsObject) {
        ChallengeSettingsManager settingsManager = challenge.getSettingsManager();
        String challengeKey = challenge.getType().name().toLowerCase();
        if (settingsManager == null) return;

        try {
            for (Setting enumValue : settingsManager.getAllSettings()) {
                if (enumValue instanceof Setting setting) {
                    String key = setting.getId();
                    SettingStateType defaultState = setting.getDefaultState();
                    if (defaultState == null) {
                        continue;
                    }
                    if (settingsObject.has(key)) {
                        String stateStr = null;
                        try {
                            stateStr = settingsObject.get(key).getAsString();
                            SettingStateType parsedState;
                            if (defaultState instanceof ConfigurableSettingState configurable) {
                                parsedState = configurable.parse(stateStr);
                            } else {
                                parsedState = parseState(defaultState.getClass(), stateStr);
                            }
                            settingsManager.setState(enumValue, Objects.requireNonNullElse(parsedState, defaultState));
                        } catch (IllegalArgumentException e) {
                            logger.warn(
                                    "Invalid state '{}' for setting {} in {}, using default state",
                                    stateStr, setting.getId(), challengeKey
                            );
                            settingsManager.setState(enumValue, defaultState);
                        }
                    } else {
                        settingsManager.setState(enumValue, defaultState);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load settings for challenge {}: {}", challengeKey, e.getMessage());
        }
    }

    private static SettingStateType parseState(Class<? extends SettingStateType> stateClass, String stateName) {
        if (stateClass == null || stateName == null) return null;
        if (!stateClass.isEnum()) return null;

        Object[] constants = stateClass.getEnumConstants();
        if (constants == null) return null;

        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant) {
                if (enumConstant.name().equalsIgnoreCase(stateName)) {
                    return (SettingStateType) constant;
                }
            }
        }
        return null;
    }

    public static void saveToFile(File jsonFile) throws IOException {
        JsonObject settingsObject = new JsonObject();

        for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
            saveChallengeSettings(challenge, settingsObject);
        }

        try (FileWriter writer = new FileWriter(jsonFile)) {
            GSON.toJson(settingsObject, writer);
        }
    }

    private static void saveChallengeSettings(Challenge challenge, JsonObject settingsObject) {
        ChallengeSettingsManager settingsManager = challenge.getSettingsManager();
        String challengeKey = challenge.getType().name().toLowerCase();
        if (settingsManager == null) return;

        try {
            for (Setting setting : settingsManager.getAllSettings()) {
                SettingStateType state = settingsManager.getState(setting);
                if (state instanceof Enum<?> enumState) {
                    settingsObject.addProperty(setting.getId(), enumState.name());
                } else if (state instanceof ConfigurableSettingState configurable) {
                    settingsObject.addProperty(setting.getId(), configurable.serialize());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to save settings for challenge {}: {}", challengeKey, e.getMessage());
        }
    }

}
