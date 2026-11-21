package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.entry.EntryResult;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public final class RejoinCommand extends KatzeCommand {
    public RejoinCommand() {
        super("rejoin", List.of(), CallerType.PLAYER, "/rejoin", PermissionDefault.TRUE, "Return to the current round");
    }

    @Override public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player) || player.getPlayer() == null) return;
        EntryResult result = GameEntryService.get().rejoin(player.getPlayer());
        if (result == EntryResult.DENIED) caller.sendMessage(Component.translatable("katze.game.limbo.rejoin_denied"));
        if (result == EntryResult.QUEUED) caller.sendMessage(Component.translatable("katze.game.limbo.rejoin_queued"));
    }

    @Override public List<String> tabComplete(CommandCaller caller, String[] args) { return List.of(); }
}
