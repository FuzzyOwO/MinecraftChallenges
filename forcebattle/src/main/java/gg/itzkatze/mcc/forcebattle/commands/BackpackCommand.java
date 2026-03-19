package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleSettings;
import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.forcebattle.ForceBattleGameListener;
import gg.itzkatze.mcc.player.MccPlayer;
import org.jetbrains.annotations.NotNull;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Optional;

public class BackpackCommand extends MccCommand {

    public BackpackCommand() {
        super(
                "backpack",
                List.of("bp"),
                CallerType.PLAYER,
                "/backpack",
                PermissionDefault.TRUE,
                "Command to open the backpack"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!SettingsManager.isEnabled(ForceBattleSettings.BACKPACK)) {
            caller.sendMessage(Component.text("Backpacks are disabled.").color(NamedTextColor.RED));
            return;
        }
        if (ForceBattleGameListener.startPhase) {
            caller.sendMessage(Component.text("The game hasn't started yet.").color(NamedTextColor.RED));
            return;
        }

        Optional<MccPlayer> senderOpt = MccPlayer.adapt(caller.getName());
        senderOpt.filter(ForceBattlePlayer.class::isInstance)
                .map(ForceBattlePlayer.class::cast)
                .ifPresent(ForceBattlePlayer::openBackpack);
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        return List.of();
    }
}
