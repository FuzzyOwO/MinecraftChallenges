package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.chat.TeamChatManager;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.player.MccPlayer;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

public class TeamChatCommand extends MccCommand {

    public TeamChatCommand() {
        super(
                "t",
                List.of(),
                CallerType.PLAYER,
                "/t chat <message>",
                PermissionDefault.TRUE,
                "Send a message directly to team chat"
        );
    }

    @Override
    protected void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("chat")) {
            sendUsage(caller);
            return;
        }

        MccPlayer player = MccPlayer.adapt(caller.getName()).orElse(null);
        if (player != null) {
            TeamChatManager.sendTeamMessage(player, Component.text(String.join(" ", args).substring(5)));
        }
    }

    @Override
    protected List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1 && "chat".startsWith(args[0].toLowerCase())) {
            return List.of("chat");
        }
        return List.of();
    }
}
