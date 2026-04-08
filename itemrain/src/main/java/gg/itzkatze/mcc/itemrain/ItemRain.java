package gg.itzkatze.mcc.itemrain;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.itemrain.gui.ItemRainSettingsGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ItemRain implements Challenge {

    private final ItemRainGameListener gameListener = new ItemRainGameListener();
    private final ChallengeSettingsManager settingsManager =
            new ChallengeSettingsManager(this, ItemRainSettings.ItemRainSetting.class);

    @Override
    public void onEnable() {
        gameListener.reset();
    }

    @Override
    public void onDisable() {
        gameListener.reset();
    }

    @Override
    public void onGameReset() {
        gameListener.reset();
    }

    @Override
    public @NotNull String getName() {
        return "itemrain";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Item Rain");
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.ITEM_RAIN;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.DROPPER;
    }

    @Override
    public @NotNull String getDescription() {
        return "Gives each player a random item every few seconds";
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
        return List.of(gameListener::runTask);
    }

    @Override
    public MccGUI getSettingsGUI() {
        return new ItemRainSettingsGUI();
    }

    @Override
    @Nullable public ChallengeSettingsManager getSettingsManager() {
        return settingsManager;
    }
}
