package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.forcebattle.gui.info.InfoGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class InformationCommand extends MccCommand {

    public InformationCommand() {
        super(
                "information",
                List.of("info", "information"),
                CallerType.PLAYER,
                "/info",
                PermissionDefault.TRUE,
                "Command to open the info gui"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        Optional<MccPlayer> senderOpt = MccPlayer.adapt(caller.getName());
        senderOpt.ifPresent(mccPlayer -> new InfoGUI().open(mccPlayer));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        return List.of();
    }
}
