package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.chat.TeamChatManager;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.spectator.SpectatorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class SpectateCommand extends MccCommand {

    public SpectateCommand() {
        super(
                "spectate",
                List.of(),
                CallerType.PLAYER,
                "/spectate",
                PermissionDefault.TRUE,
                "Spectate the next battle instead of playing in it"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof MccPlayer player)) return;

        if (ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.text("You can't switch during a battle. After /reset you can choose again.", NamedTextColor.RED));
            return;
        }
        if (player.isSpectator()) {
            SpectatorManager.remove(player.getUuid());
            caller.sendMessage(Component.text("You will play in the next battle.", NamedTextColor.GREEN));
            return;
        }
        SpectatorManager.choose(player.getUuid());
        player.leaveTeam();
        TeamChatManager.setTeamChatEnabled(player, false);
        caller.sendMessage(Component.text("You will spectate the next battle. Type /spectate again to play.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
