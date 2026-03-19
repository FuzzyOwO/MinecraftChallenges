package gg.itzkatze.mcc;

import lombok.Getter;
import gg.itzkatze.mcc.bossbar.BossbarManager;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandHandler;
import gg.itzkatze.mcc.commands.framework.CommandVisibilityListener;
import gg.itzkatze.mcc.game.SpawnManager;
import gg.itzkatze.mcc.configuration.SettingsFile;
import gg.itzkatze.mcc.game.MccTimer;
import gg.itzkatze.mcc.scoreboard.ScoreboardManager;
import gg.itzkatze.mcc.util.EventRegistrar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Stream;

public final class MinecraftChallenges extends JavaPlugin {

    private static final Logger logger = LoggerFactory.getLogger("MinecraftChallenges");
    private static MinecraftChallenges instance;
    @Getter
    private static BukkitAudiences bukkitAudiences;

    @Getter
    private static MccTimer timer;

    public static MinecraftChallenges get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        bukkitAudiences = BukkitAudiences.create(this);

        saveDefaultConfig();
        File settingsFile = new File(getDataDirectory(), "data/settings.json");
        if (!settingsFile.exists()) {
            File parent = settingsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            saveResource("data/settings.json", false);
        }

        timer = new MccTimer(getGameDuration(), false);

        try {
            SettingsFile.loadSettingsFromFile(settingsFile);
        } catch (IOException e) {
            throw new RuntimeException("settings.json File not found", e);
        }

        CommandHandler.registerAll(loopThroughPackage("gg.itzkatze.mcc.commands", MccCommand.class).toList());
        getServer().getPluginManager().registerEvents(new CommandVisibilityListener(), this);
        BossbarManager.runTask();
        ScoreboardManager.startUpdater();

        EventRegistrar.registerAll(this, "gg.itzkatze.mcc");

        getServer().getPluginManager().registerEvents(new SpawnManager(), this);

        logger.info("Successfully initialized MinecraftChallenges.");
    }

    @Override
    public void onDisable() {
        try {
            SettingsFile.saveToFile(new File(getDataDirectory(), "data/settings.json"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings to file", e);
        }
        getConfig().set("first-startup", false);
        saveConfig();
    }

    public @NotNull File getDataDirectory() {
        return getDataFolder();
    }

    public int getGameDuration() {
        return getConfig().getInt("game-duration", 5400);
    }

    public static void broadcast(Component message) {
        if (bukkitAudiences != null) {
            bukkitAudiences.all().sendMessage(message);
        }
    }

    /**
     * Scans a package for subtypes of the given class and instantiates them.
     * Used for core-only discovery (e.g. commands in core module).
     */
    public static <T> Stream<T> loopThroughPackage(String packageName, Class<T> clazz) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(clazz);

        return subTypes.stream()
                .map(subClass -> {
                    try {
                        return clazz.cast(subClass.getDeclaredConstructor().newInstance());
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                             InvocationTargetException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull);
    }
}
