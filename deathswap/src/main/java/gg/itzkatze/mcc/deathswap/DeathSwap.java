package gg.itzkatze.mcc.deathswap;

import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.deathswap.gui.DeathSwapSettingsGUI;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DeathSwap implements Challenge {

    private final DeathSwapGameListener gameListener = new DeathSwapGameListener();
    private final ChallengeSettingsManager settingsManager =
            new ChallengeSettingsManager(this, DeathSwapSettings.DeathSwapSetting.class);

    @Override
    public void onEnable() {
        gameListener.initialize();
    }

    @Override
    public void onDisable() {
        gameListener.cleanup();
    }

    @Override
    public void onGameStart() {
        gameListener.startGame();
    }

    @Override
    public void onGameReset() {
        gameListener.cleanup();
        gameListener.initialize();
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.DEATH_SWAP;
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
    public @Nullable Component getActionbar(@NotNull MccPlayer player, boolean timerRunning, int timerSeconds) {
        return gameListener.getActionbar(player, timerRunning);
    }

    @Override
    public int getActionbarPriority() {
        return 100;
    }

    @Override
    public MccGUI getSettingsGUI() {
        return new DeathSwapSettingsGUI();
    }

    @Override
    @Nullable public ChallengeSettingsManager getSettingsManager() {
        return settingsManager;
    }
}
