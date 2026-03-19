package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.gui.impl.SettingsGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SettingsCommand extends MccCommand {

    public SettingsCommand() {
        super(
                "settings",
                List.of("menu"),
                CallerType.PLAYER,
                "/settings",
                PermissionDefault.OP,
                "Command to open the settings menu"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (caller instanceof MccPlayer player) {
            new SettingsGUI().open(player);
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }

}
