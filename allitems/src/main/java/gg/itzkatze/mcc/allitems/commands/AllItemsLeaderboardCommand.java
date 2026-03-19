package gg.itzkatze.mcc.allitems.commands;

import gg.itzkatze.mcc.allitems.gui.AllItemsLeaderboardGUI;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AllItemsLeaderboardCommand extends MccCommand {

    public AllItemsLeaderboardCommand() {
        super(
                "leaderboard",
                List.of(),
                CallerType.PLAYER,
                "/leaderboard",
                PermissionDefault.TRUE,
                "Open the All Items leaderboard"
        );
    }

    @Override
    protected void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (caller instanceof MccPlayer player) {
            new AllItemsLeaderboardGUI().open(player);
        } else {
            caller.sendMessage(Component.text("This command can only be used by players."));
        }
    }

    @Override
    protected List<String> tabComplete(@NotNull CommandCaller caller, String[] args) {
        return List.of();
    }
}
