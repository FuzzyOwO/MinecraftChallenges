package gg.itzkatze.mcc.randomizer;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.randomizer.gui.RandomizerSettingsGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class Randomizer implements Challenge {

    private final RandomizerGameListener gameListener = new RandomizerGameListener();
    private final ChallengeSettingsManager settingsManager =
            new ChallengeSettingsManager(this, RandomizerSettings.RandomizerSetting.class);

    @Override
    public void onEnable() {
        gameListener.resetMappings();
    }

    @Override
    public void onDisable() {
        gameListener.resetMappings();
    }

    @Override
    public void onGameReset() {
        gameListener.resetMappings();
    }

    @Override
    public @NotNull String getName() {
        return "randomizer";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Randomizer");
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.RANDOMIZER;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.SNIFFER_EGG;
    }

    @Override
    public @NotNull String getDescription() {
        return "Randomizes drops and chest loot";
    }

    @Override
    public @NotNull List<MccCommand> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull List<Listener> getListeners() {
        return List.of(gameListener);
    }

    @Override
    public @NotNull List<Runnable> getTasks() {
        return Collections.emptyList();
    }

    @Override
    public MccGUI getSettingsGUI() {
        return new RandomizerSettingsGUI();
    }

    @Override
    @Nullable public ChallengeSettingsManager getSettingsManager() {
        return settingsManager;
    }
}
