package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.SavedResultsGUI;
import gg.itzkatze.katze.replay.ReplayService;
import gg.itzkatze.katze.replay.ReplaySession;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Locale;

public final class ReplaysCommand extends KatzeCommand {
    public ReplaysCommand() {
        super("replays", List.of(), CallerType.PLAYER, "/replays [join <player>|leave|pause|speed|seek <seconds>]", PermissionDefault.TRUE,
                "Browse saved rounds you played in");
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player)) return;
        ReplayService replays = ReplayService.get();
        ReplaySession session = replays.session(player.getUuid());
        if (args.length == 0) {
            if (session == null) new SavedResultsGUI().open(player);
            else session.showControls();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            replays.join(player.getPlayer(), args[1]);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            replays.leave(player.getPlayer());
        } else if (session != null && args.length == 1 && args[0].equalsIgnoreCase("pause")) {
            session.togglePause();
        } else if (session != null && args.length == 1 && args[0].equalsIgnoreCase("speed")) {
            session.cycleSpeed();
        } else if (session != null && args.length == 2 && args[0].equalsIgnoreCase("seek")) {
            try { session.seek(Integer.parseInt(args[1])); }
            catch (NumberFormatException e) { sendUsage(caller); }
        } else sendUsage(caller);
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("join") && caller instanceof KatzePlayer player && ReplayService.get() != null) {
            return ReplayService.get().joinablePlayers(player.getUuid()).stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return args.length == 1 ? List.of("join", "leave", "pause", "speed", "seek").stream()
                .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList() : List.of();
    }
}
