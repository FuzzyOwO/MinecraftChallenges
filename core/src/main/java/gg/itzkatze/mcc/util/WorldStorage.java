package gg.itzkatze.mcc.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gg.itzkatze.mcc.MinecraftChallenges;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class WorldStorage {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final String DATA_DIR_NAME = "mcc_data";

    private WorldStorage() {
    }

    public static Path getFilePath(String fileName) {
        World mainWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        File baseDir = (mainWorld != null) ? mainWorld.getWorldFolder() : MinecraftChallenges.get().getDataDirectory();
        File dataDir = new File(baseDir, DATA_DIR_NAME);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return new File(dataDir, fileName).toPath();
    }

    public static <T> void save(String fileName, T object) {
        Path path = getFilePath(fileName);
        Path tempPath = path.resolveSibling(fileName + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            MinecraftChallenges.get().getLogger().severe("Failed to save " + fileName + ": " + e.getMessage());
        }
    }

    public static <T> T load(String fileName, Class<T> clazz) {
        Path path = getFilePath(fileName);
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, clazz);
        } catch (Exception e) {
            MinecraftChallenges.get().getLogger().warning("Failed to load " + fileName + ", starting fresh: " + e.getMessage());
            return null;
        }
    }

    public static boolean exists(String fileName) {
        return Files.exists(getFilePath(fileName));
    }

    public static void delete(String fileName) {
        Path path = getFilePath(fileName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
