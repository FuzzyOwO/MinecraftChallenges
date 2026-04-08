package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.game.GameLifecycleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

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
        if (ChallengeManager.getActiveChallenges().isEmpty()) {
            caller.sendMessage(Component.text("No challenge is enabled, game can not be started!").color(NamedTextColor.RED));
            return;
        }
        if (ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.text("The game has already been started.").color(NamedTextColor.RED));
            return;
        }

        GameLifecycleManager.startGame();
        ChallengeManager.startActiveChallenges();
        MinecraftChallenges.getTimer().setTime(MinecraftChallenges.getTimer().getStartTime());
        MinecraftChallenges.getTimer().start();
        caller.sendMessage(Component.text("The game has been started.").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
