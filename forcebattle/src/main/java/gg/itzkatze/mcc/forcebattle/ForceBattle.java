package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.bossbar.BossbarManager;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.challenges.TimerSpec;
import gg.itzkatze.mcc.game.GameTimer;
import gg.itzkatze.mcc.game.SessionPersistence;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattlePlayerObjectiveBossbar;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattleTeamObjectiveBossbar;
import gg.itzkatze.mcc.forcebattle.bossbar.ForceBattleWaitingBossbar;
import gg.itzkatze.mcc.forcebattle.gui.ForceBattleSettingsGUI;
import gg.itzkatze.mcc.forcebattle.tasks.StructureTask;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.configuration.ChallengeSettingsManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ForceBattle implements Challenge {
    public static final String BATTLE_DURATION_MINUTES_KEY = "forcebattle-duration-minutes";
    public static final int DEFAULT_BATTLE_DURATION_MINUTES = 90;


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
        loadData();
    }

    @Override
    public void onDisable() {
        saveData();
        ForceBattleGameListener.startPhase = true;
        gameListener.clearPendingRestores();
        NametagManager.stopTask();
        StructureTask.clearCache();
        ForceBattleObjective.deleteAll();
        BossbarManager.hideBarsForChallenge(this);
        ForceBattleTeam.deleteAll();
        MccTeam.downgrade(ChallengeType.FORCE_BATTLE);
        MccTeam.unregisterFactory(ChallengeType.FORCE_BATTLE);
        MccPlayer.downgrade(ChallengeType.FORCE_BATTLE);
        MccPlayer.unregisterFactory(ChallengeType.FORCE_BATTLE);
    }

    @Override
    public void saveData() {
        ForceBattlePersistence.saveData();
    }

    @Override
    public void loadData() {
        ForceBattlePersistence.loadData();
    }

    @Override
    public void onGameStart() {
        Bukkit.getWorlds().forEach(world ->
                world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false));
        gameListener.startGame();
    }

    @Override
    public void onGameReset() {
        ForceBattlePersistence.deleteData();
        ForceBattleGameListener.startPhase = true;
        gameListener.clearPendingRestores();
        StructureTask.clearCache();
        ForceBattleObjective.deleteAll();
        ForceBattleObjectiveManager.getInstance().clearChains();
        MccTeam.teams.stream()
                .filter(ForceBattleTeam.class::isInstance)
                .map(ForceBattleTeam.class::cast)
                .forEach(team -> team.setObjective(null));
        MccPlayer.PLAYERS.stream()
                .filter(ForceBattlePlayer.class::isInstance)
                .map(ForceBattlePlayer.class::cast)
                .filter(player -> !player.isOffline())
                .forEach(player -> player.reset(true));
    }

    @Override
    public @Nullable TimerSpec getTimerSpec() {
        int minutes = Math.max(1, MinecraftChallenges.get().getConfig().getInt(
                BATTLE_DURATION_MINUTES_KEY,
                DEFAULT_BATTLE_DURATION_MINUTES
        ));
        int seconds = minutes * 60;
        return new TimerSpec(GameTimer.Mode.COUNT_DOWN, seconds);
    }

    @Override
    public void onTimerElapsed() {
        gameListener.handleBattleEnd();
        SessionPersistence.requestSave();
    }

    @Override
    public @NotNull ChallengeType getType() {
        return ChallengeType.FORCE_BATTLE;
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
