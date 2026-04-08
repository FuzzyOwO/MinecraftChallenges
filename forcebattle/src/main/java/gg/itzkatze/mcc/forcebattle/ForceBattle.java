package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.bossbar.BossbarManager;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattlePlayerObjectiveBossbar;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattleTeamObjectiveBossbar;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattleWaitingBossbar;
import gg.itzkatze.mcc.forcebattle.gui.ForceBattleSettingsGUI;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ForceBattle implements Challenge {

    private final ForceBattleGameListener gameListener = new ForceBattleGameListener();
    private final ChallengeSettingsManager settingsManager =
            new ChallengeSettingsManager(this, ForceBattleSettings.class);

    @Override
    public void onEnable() {
        MccPlayer.registerFactory(ChallengeType.FORCE_BATTLE, ForceBattlePlayer::new);
        MccPlayer.upgrade(ChallengeType.FORCE_BATTLE);
        MccTeam.registerFactory(ChallengeType.FORCE_BATTLE, ForceBattleTeam::new);
        MccTeam.upgrade(ChallengeType.FORCE_BATTLE);
        NametagManager.runTask();
    }

    @Override
    public void onDisable() {
        ForceBattleGameListener.startPhase = true;
        BossbarManager.hideBarsForChallenge(this);
        ForceBattleTeam.deleteAll();
        MccTeam.downgrade(ChallengeType.FORCE_BATTLE);
        MccTeam.unregisterFactory(ChallengeType.FORCE_BATTLE);
        MccPlayer.downgrade(ChallengeType.FORCE_BATTLE);
        MccPlayer.unregisterFactory(ChallengeType.FORCE_BATTLE);
    }

    @Override
    public void onGameStart() {
        gameListener.startGame();
    }

    @Override
    public void onGameReset() {
        ForceBattleGameListener.startPhase = true;
        MccPlayer.PLAYERS.stream()
                .filter(ForceBattlePlayer.class::isInstance)
                .map(ForceBattlePlayer.class::cast)
                .filter(player -> !player.isOffline())
                .forEach(player -> player.reset(true));
    }

    @Override
    @NotNull public String getName() {
        return "forcebattle";
    }

    @Override
    @NotNull public Component getDisplayName() {
        return Component.text("Force Battle");
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.FORCE_BATTLE;
    }

    @Override
    @NotNull public Material getMaterial() {
        return Material.DIAMOND_SWORD;
    }

    @Override
    @NotNull public String getDescription() {
        return "Complete random objectives to score points";
    }

    @Override
    @NotNull public List<MccCommand> getCommands() {
        return gg.itzkatze.mcc.MinecraftChallenges.loopThroughPackage(
                "gg.itzkatze.mcc.forcebattle.commands",
                MccCommand.class
        ).toList();
    }

    @Override
    @NotNull public List<Listener> getListeners() {
        return List.of(gameListener);
    }

    @Override
    @NotNull public List<Runnable> getTasks() {
        return List.of(gameListener::runTasks);
    }

    @Override
    public ScoreboardContent getScoreboardContent() {
        return new ForceBattleScoreboardContent();
    }

    @Override
    public @NonNull List<BossbarContent> getBossbarContents() {
        return List.of(
                new ForceBattleWaitingBossbar(),
                new ForceBattlePlayerObjectiveBossbar(),
                new ForceBattleTeamObjectiveBossbar()
        );
    }

    @Override
    public MccGUI getSettingsGUI() {
        return new ForceBattleSettingsGUI();
    }

    @Override
    @Nullable public ChallengeSettingsManager getSettingsManager() {
        return settingsManager;
    }
}
