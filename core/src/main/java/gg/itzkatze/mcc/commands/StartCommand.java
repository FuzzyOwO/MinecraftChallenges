package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.game.GameLifecycleManager;
import gg.itzkatze.mcc.game.GameTimer;
import gg.itzkatze.mcc.game.SessionPersistence;
import gg.itzkatze.mcc.game.StartCountdown;
import gg.itzkatze.mcc.spectator.SpectatorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class StartCommand extends MccCommand {

    public StartCommand() {
        super(
                "start",
                List.of(),
                CallerType.NONE,
                "/start",
                PermissionDefault.OP,
                "Command to start the configured game"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (StartCountdown.isRunning()) {
            caller.sendMessage(Component.text("The game is already starting.").color(NamedTextColor.RED));
            return;
        }
        if (!canStart(caller)) return;

        caller.sendMessage(Component.text("The game starts in " + StartCountdown.SECONDS + " seconds.").color(NamedTextColor.GREEN));
        StartCountdown.begin(() -> start(caller));
    }

    private static boolean canStart(@NotNull CommandCaller caller) {
        if (ChallengeManager.getActiveChallenges().isEmpty()) {
            caller.sendMessage(Component.text("No challenge is enabled, game can not be started!").color(NamedTextColor.RED));
            return false;
        }
        if (ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.text("The game has already been started.").color(NamedTextColor.RED));
            return false;
        }
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (!online.isEmpty() && online.stream().allMatch(player -> SpectatorManager.isSpectator(player.getUniqueId()))) {
            caller.sendMessage(Component.text("Nobody would play: every online player is spectating.").color(NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private static void start(@NotNull CommandCaller caller) {
        if (!canStart(caller)) return;

        GameLifecycleManager.startGame();
        if (!ChallengeManager.startActiveChallenges()) {
            GameLifecycleManager.resetGame();
            String reason = ChallengeManager.getLastStartError();
            caller.sendMessage(Component.text(reason == null ? "Failed to start challenges." : reason).color(NamedTextColor.RED));
            return;
        }
        if (MinecraftChallenges.getTimer().getMode() != GameTimer.Mode.DISABLED) {
            MinecraftChallenges.getTimer().start();
        }
        SessionPersistence.checkpoint();
        caller.sendMessage(Component.text("The game has been started.").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
