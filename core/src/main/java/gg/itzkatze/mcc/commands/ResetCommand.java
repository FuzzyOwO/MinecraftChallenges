package gg.itzkatze.mcc.commands;

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

public class ResetCommand extends MccCommand {

    public ResetCommand() {
        super(
                "reset",
                List.of(),
                CallerType.NONE,
                "/reset",
                PermissionDefault.OP,
                "Command to reset the configured game"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (ChallengeManager.getActiveChallenges().isEmpty()) {
            caller.sendMessage(Component.text("No challenge is enabled, nothing to reset.").color(NamedTextColor.RED));
            return;
        }

        GameLifecycleManager.resetGame();
        caller.sendMessage(Component.text("The game has been reset.").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
