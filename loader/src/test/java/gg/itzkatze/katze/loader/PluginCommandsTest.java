package gg.itzkatze.katze.loader;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.io.InputStream;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PluginCommandsTest {
    @Test void packagedPluginIncludesLosslessCompressionAndItsServerPlatformLibraries() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile())) {
            assertNotNull(jar.getJarEntry("com/github/luben/zstd/ZstdInputStream.class"));
            assertTrue(jar.stream().anyMatch(entry -> entry.getName().startsWith("linux/amd64/libzstd") && entry.getName().endsWith(".so")));
            assertTrue(jar.stream().anyMatch(entry -> entry.getName().startsWith("linux/aarch64/libzstd") && entry.getName().endsWith(".so")));
            assertTrue(jar.stream().anyMatch(entry -> entry.getName().startsWith("win/amd64/libzstd") && entry.getName().endsWith(".dll")));
        }
    }

    @Test void packagedPluginUsesReplaysWithoutRegisteringTheOldResultsCommand() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile());
             var input = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            assertTrue(description.getCommands().containsKey("replays"));
            assertFalse(description.getCommands().containsKey("results"));
            assertNotNull(jar.getJarEntry("gg/itzkatze/katze/commands/ReplaysCommand.class"));
            assertNull(jar.getJarEntry("gg/itzkatze/katze/commands/ResultsCommand.class"));
        }
    }

    @Test void packagedPluginIncludesTheCustomArenaAndItsNbtReader() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile())) {
            assertNotNull(jar.getJarEntry("arena/village.schem"));
            assertNotNull(jar.getJarEntry("net/kyori/adventure/nbt/BinaryTagIO.class"));
        }
    }

    @Test void packagedPluginDeclaresTheWavesCommandForPaperRegistration() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile());
             var input = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            assertTrue(description.getCommands().containsKey("waves"));
            assertTrue(description.getCommands().containsKey("tracker"));
            assertTrue(description.getCommands().containsKey("watch"));
        }
    }
    @Test void packagedPluginIncludesEntryCommands() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile());
             InputStream input = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            assertTrue(description.getCommands().containsKey("rejoin"));
            assertTrue(description.getCommands().containsKey("limbo"));
            assertNotNull(jar.getJarEntry("gg/itzkatze/katze/commands/RejoinCommand.class"));
            assertNotNull(jar.getJarEntry("gg/itzkatze/katze/commands/LimboCommand.class"));
        }
    }
}
