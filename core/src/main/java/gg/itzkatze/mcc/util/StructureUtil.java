package gg.itzkatze.mcc.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class StructureUtil {

    private static final String NAMESPACE = "mcc";

    public static @Nullable Structure loadStructure(@NotNull String name) {
        StructureManager manager = Bukkit.getStructureManager();
        NamespacedKey key = new NamespacedKey(NAMESPACE, name);
        Structure cached = manager.getStructure(key);
        if (cached != null) {
            return cached;
        }

        try (InputStream in = StructureUtil.class.getResourceAsStream("/structures/" + name + ".nbt")) {
            if (in == null) {
                Bukkit.getLogger().severe("Could not find structure file: " + name);
                return null;
            }
            Structure structure = manager.loadStructure(in);
            manager.registerStructure(key, structure);
            return structure;
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not load structure " + name + ": " + e.getMessage());
            return null;
        }
    }
}
