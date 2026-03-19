package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MobileCraftingCommand extends MccCommand {

    public MobileCraftingCommand() {
        super(
                "craft",
                List.of("crafting"),
                CallerType.PLAYER,
                "/craft",
                PermissionDefault.TRUE,
                "Command to open the crafting table GUI"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (!SettingsManager.isEnabled(GlobalSetting.MOBILE_CRAFTING)) {
            caller.sendMessage(Component.text("Mobile Crafting is disabled.").color(NamedTextColor.RED));
            return;
        }
        if (caller instanceof MccPlayer mccPlayer) {
            Player player = mccPlayer.getPlayer();
            player.openWorkbench(null, true);
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
