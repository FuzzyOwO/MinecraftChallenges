package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.game.MccTimer;
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
                "/timer <toggle|set|duration> <time in s>",
                PermissionDefault.OP,
                "Command to control the timer: toggle, set, duration"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 1) {
            sendUsage(caller);
            return;
        }
        MccTimer timer = MinecraftChallenges.getTimer();
        switch (args[0]) {
            case "toggle" -> {
                if (timer.isRunning()) {
                    timer.pause();
                    caller.sendMessage(Component.text("The timer has been paused.").color(NamedTextColor.BLUE));
                } else {
                    if (!ChallengeManager.isGameStarted()) {
                        caller.sendMessage(Component.text("The game has not been started yet. Use /start first.").color(NamedTextColor.RED));
                        return;
                    }
                    timer.start();
                    caller.sendMessage(Component.text("The timer has been started.").color(NamedTextColor.GREEN));
                }
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
                timer.setTime(newTime);
                caller.sendMessage(Component.text("The timer has been set to " + StringUtility.formatTime(newTime) + ".").color(NamedTextColor.BLUE));
                return;
            }
            case "duration" -> {
                if (args.length < 2) {
                    sendUsage(caller);
                    return;
                }
                Integer newStartTime = parseInt(caller, args[1]);
                if (newStartTime == null) {
                    return;
                }
                timer.setStartTime(newStartTime);
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
            return StringUtility.copyPartialMatches(args[0], List.of("toggle", "set", "duration"), new ArrayList<>());
        }
        return List.of();
    }
}
