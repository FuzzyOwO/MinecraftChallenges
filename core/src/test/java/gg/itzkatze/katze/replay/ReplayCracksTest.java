package gg.itzkatze.katze.replay;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayCracksTest {
    @Test void progressIsClearedOnAbortAndSeekAndRefreshedWhilePaused() {
        UUID dimension = UUID.randomUUID(), miner = UUID.randomUUID();
        World world = world();
        List<Float> progress = new ArrayList<>();
        Player viewer = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "sendBlockDamage" -> { progress.add((float) args[1]); yield null; }
            default -> throw new AssertionError(method.getName());
        });
        ReplayCracks cracks = new ReplayCracks(viewer, id -> world);
        var attempt = new ReplayFrame.Mining(miner, dimension, 0, 64, 0, 0.5f);
        cracks.render(List.of(attempt), 1);
        cracks.render(List.of(attempt), 2);
        assertEquals(List.of(0.5f), progress);
        cracks.render(List.of(attempt), 100);
        assertEquals(List.of(0.5f, 0.5f), progress);
        cracks.render(List.of(), 101);
        assertEquals(0f, progress.getLast());
        cracks.render(List.of(attempt), 102);
        cracks.clear();
        assertEquals(0f, progress.getLast());
    }

    @Test void cracksInAnotherDimensionAreNotSentToTheViewer() {
        World viewerWorld = world(), other = world();
        Player viewer = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
            if (method.getName().equals("getWorld")) return viewerWorld;
            throw new AssertionError("Unexpected packet: " + method.getName());
        });
        new ReplayCracks(viewer, id -> other).render(List.of(new ReplayFrame.Mining(UUID.randomUUID(), UUID.randomUUID(), 0, 64, 0, 0.5f)), 1);
    }

    private World world() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            throw new AssertionError(method.getName());
        });
    }
}
