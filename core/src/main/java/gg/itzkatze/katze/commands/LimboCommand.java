package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.game.entry.LeaveReason;
import gg.itzkatze.katze.player.KatzePlayer;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public final class LimboCommand extends KatzeCommand {
    public LimboCommand() {
        super("limbo", List.of(), CallerType.PLAYER, "/limbo", PermissionDefault.TRUE, "Return to the lobby");
    }

    @Override public void executeCommand(CommandCaller caller, String[] args) {
        if (caller instanceof KatzePlayer player && player.getPlayer() != null) {
            GameEntryService.get().enterLimbo(player.getPlayer(), LeaveReason.LIMBO);
        }
    }

    @Override public List<String> tabComplete(CommandCaller caller, String[] args) { return List.of(); }
}
