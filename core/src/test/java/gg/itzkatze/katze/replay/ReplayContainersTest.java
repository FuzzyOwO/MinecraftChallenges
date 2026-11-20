package gg.itzkatze.katze.replay;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayContainersTest {
    @Test void pausedFramesKeepContainersOpenUntilTheLastViewerClosesThem() {
        UUID world = UUID.randomUUID();
        var chest = new ReplayFrame.Container(world, -1, 64, 3);
        var calls = new ArrayList<String>();
        var containers = new ReplayContainers(id -> world(calls));
        containers.render(List.of(chest, chest));
        containers.render(List.of(chest));
        containers.render(List.of(chest));
        assertEquals(List.of("open"), calls);
        containers.render(List.of());
        containers.render(List.of());
        assertEquals(List.of("open", "close"), calls);
    }

    @Test void seekingResetsLidsBeforeRestoringTheTargetFramesState() {
        UUID world = UUID.randomUUID();
        var chest = new ReplayFrame.Container(world, 0, 64, 0);
        var calls = new ArrayList<String>();
        var containers = new ReplayContainers(id -> world(calls));
        containers.render(List.of(chest));
        containers.clear();
        containers.render(List.of(chest));
        containers.clear();
        containers.render(List.of());
        assertEquals(List.of("open", "close", "open", "close"), calls);
    }

    private World world(List<String> calls) {
        Chest lid = (Chest) Proxy.newProxyInstance(Chest.class.getClassLoader(), new Class<?>[]{Chest.class}, (proxy, method, args) -> {
            calls.add(method.getName());
            return null;
        });
        Block block = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class}, (proxy, method, args) -> {
            assertEquals("getState", method.getName());
            return lid;
        });
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isChunkLoaded" -> true;
            case "getBlockAt" -> block;
            default -> throw new AssertionError(method.getName());
        });
    }
}
