package gg.itzkatze.katze.replay;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayContainerSnapshotsTest {
    @Test void sharedChestRemainsOpenUntilEveryPlayerStopsViewingIt() {
        UUID worldId = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        World world = world(worldId);
        Block block = block(worldId, Material.CHEST);
        Chest chest = (Chest) Proxy.newProxyInstance(Chest.class.getClassLoader(), new Class<?>[]{Chest.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isPlaced" -> true;
            case "getBlock" -> block;
            default -> throw new AssertionError(method.getName());
        });
        var snapshots = new ReplayContainerSnapshots();
        var firstOpen = new ReplayContainerSnapshots.View(first, world, chest, false);
        var secondOpen = new ReplayContainerSnapshots.View(second, world, chest, false);
        var firstClosed = new ReplayContainerSnapshots.View(first, world, null, false);
        var secondClosed = new ReplayContainerSnapshots.View(second, world, null, false);
        var expected = List.of(new ReplayFrame.Container(worldId, -1, 64, 0));
        assertEquals(expected, snapshots.captureViews(List.of(firstOpen, secondOpen)));
        assertEquals(expected, snapshots.captureViews(List.of(firstClosed, secondOpen)));
        assertTrue(snapshots.captureViews(List.of(firstClosed, secondClosed)).isEmpty());
    }

    @Test void enderChestRequiresAnActualOpenInventoryAndClosesAfterLeavingIt() {
        UUID worldId = UUID.randomUUID(), player = UUID.randomUUID();
        var closed = new ReplayContainerSnapshots.View(player, world(worldId), null, false);
        var open = new ReplayContainerSnapshots.View(player, world(worldId), null, true);
        Block chest = block(worldId, Material.ENDER_CHEST);
        var snapshots = new ReplayContainerSnapshots();
        snapshots.interact(player, chest);
        assertTrue(snapshots.captureViews(List.of(closed)).isEmpty());
        snapshots.interact(player, chest);
        assertEquals(List.of(new ReplayFrame.Container(worldId, -1, 64, 0)), snapshots.captureViews(List.of(open)));
        assertTrue(snapshots.captureViews(List.of(closed)).isEmpty());
        assertTrue(snapshots.captureViews(List.of(open)).isEmpty());
    }

    private World world(UUID id) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> id;
            case "isChunkLoaded" -> true;
            case "getBlockAt" -> block(id, Material.ENDER_CHEST);
            default -> throw new AssertionError(method.getName());
        });
    }

    private Block block(UUID world, Material type) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getType" -> type;
            case "getWorld" -> world(world);
            case "getX" -> -1;
            case "getY" -> 64;
            case "getZ" -> 0;
            default -> throw new AssertionError(method.getName());
        });
    }
}
