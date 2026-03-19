package gg.itzkatze.mcc.commands.framework;

import gg.itzkatze.mcc.MinecraftChallenges;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

public class BukkitConsoleCommandCaller implements ConsoleCommandCaller {

    private static BukkitConsoleCommandCaller instance;

    public static BukkitConsoleCommandCaller instance() {
        if (instance == null) {
            instance = new BukkitConsoleCommandCaller();
        }
        return instance;
    }

    @Override
    public CallerType callerType() {
        return CallerType.CONSOLE;
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(Component message) {
        MinecraftChallenges.getBukkitAudiences().console().sendMessage(message);
    }

    @Override
    public boolean hasPermission(PermissionDefault permission) {
        return permission.getValue(true);
    }

}
