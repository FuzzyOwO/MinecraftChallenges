package gg.itzkatze.katze.util;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldStorageCheckedTest {
    @Test void failedSessionWriteIsNotSilentlyIgnored(@TempDir Path directory) throws Exception {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorldFolder")) return directory.toFile();
                    throw new AssertionError(method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            Files.createDirectories(directory.resolve("katze_data/session.json"));
            assertThrows(RuntimeException.class, () -> WorldStorage.saveChecked("session.json", new Object()));
        } finally {
            field.set(null, previous);
        }
    }

    @Test void malformedSessionIsNotTreatedAsMissing(@TempDir Path directory) throws Exception {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorldFolder")) return directory.toFile();
                    throw new AssertionError(method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            Path data = Files.createDirectories(directory.resolve("katze_data"));
            Files.writeString(data.resolve("session.json"), "{broken");
            assertThrows(RuntimeException.class, () -> WorldStorage.loadChecked("session.json", Object.class));
        } finally {
            field.set(null, previous);
        }
    }
}
