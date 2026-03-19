package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.forcebattle.gui.ForceBattleSettingsGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ConfigSettingsCommand extends MccCommand {

    public ConfigSettingsCommand() {
        super(
                "configsettings",
                List.of("menu"),
                CallerType.PLAYER,
                "/configsettings",
                PermissionDefault.OP,
                "Command to open the settings menu"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (caller instanceof MccPlayer player) {
            new ForceBattleSettingsGUI().open(player);
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }

}
