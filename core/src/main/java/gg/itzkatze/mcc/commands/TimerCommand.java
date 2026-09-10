package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.game.GameTimer;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TimerCommand extends MccCommand {

    public TimerCommand() {
        super(
                "timer",
                List.of(),
                CallerType.NONE,
                "/timer <pause|resume|set> [time in s]",
                PermissionDefault.OP,
                "Command to control the timer: pause, resume, set"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 1) {
            sendUsage(caller);
            return;
        }
        GameTimer timer = MinecraftChallenges.getTimer();
        switch (args[0].toLowerCase()) {
            case "pause" -> {
                timer.pause();
                caller.sendMessage(Component.text("The timer has been paused.").color(NamedTextColor.BLUE));
                return;
            }
            case "resume" -> {
                if (!ChallengeManager.isGameStarted()) {
                    caller.sendMessage(Component.text("The game has not been started yet. Use /start first.").color(NamedTextColor.RED));
                    return;
                }
                timer.start();
                caller.sendMessage(Component.text("The timer has been resumed.").color(NamedTextColor.GREEN));
                return;
            }
            case "set" -> {
                if (args.length < 2) {
                    sendUsage(caller);
                    return;
                }
                Integer newTime = parseInt(caller, args[1]);
                if (newTime == null) {
                    return;
                }
                timer.setRemainingSeconds(newTime);
                caller.sendMessage(Component.text("The timer has been set to " + StringUtility.formatTime(newTime) + ".").color(NamedTextColor.BLUE));
                return;
            }
        }
        sendUsage(caller);
    }

    private @Nullable Integer parseInt(CommandCaller caller, String input) {
        int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
            return null;
        }
        return number;
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("pause", "resume", "set"), new ArrayList<>());
        }
        return List.of();
    }
}
