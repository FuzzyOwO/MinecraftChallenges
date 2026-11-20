package gg.itzkatze.katze.replay;

import io.papermc.paper.event.entity.EntityBreakEvent;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.TagStringIO;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayEntitySnapshotsTest {
    @Test void oldHangingSnapshotsReceiveBothPositionAndBlockAttachmentBeforeLoading() throws Exception {
        for (String type : List.of("PAINTING", "ITEM_FRAME", "GLOW_ITEM_FRAME", "LEASH_KNOT")) {
            var definition = actor(type, "{id:'minecraft:item_frame',facing:2b,Item:{id:'minecraft:diamond',count:1}}", -200.25, 90.5, 400.75);
            var state = actor(type, null, -200.25, 90.5, 400.75);
            var repaired = TagStringIO.tagStringIO().asCompound(ReplayEntitySnapshots.prepare(definition, state));
            assertArrayEquals(new int[]{-201, 90, 400}, repaired.getIntArray("block_pos"));
            var position = repaired.getList("Pos", BinaryTagTypes.DOUBLE);
            assertEquals(-200.25, position.getDouble(0));
            assertEquals(90.5, position.getDouble(1));
            assertEquals(400.75, position.getDouble(2));
            assertEquals(2, repaired.getByte("facing"));
            assertEquals("minecraft:diamond", repaired.getCompound("Item").getString("id"));
            assertEquals(1, repaired.getCompound("Item").getInt("count"));
        }
    }

    @Test void validAttachmentOffsetsArePreservedWhenTheRecordedActorMoves() throws Exception {
        var definition = actor("PAINTING", "{id:'minecraft:painting',block_pos:[I;11,90,20],variant:'minecraft:pool'}", 10.5, 90.5, 20.5);
        var state = actor("PAINTING", null, 110.5, 95.5, -79.5);
        var repaired = TagStringIO.tagStringIO().asCompound(ReplayEntitySnapshots.prepare(definition, state));
        assertArrayEquals(new int[]{111, 95, -80}, repaired.getIntArray("block_pos"));
        assertEquals("minecraft:pool", repaired.getString("variant"));
    }

    @Test void staleOrMalformedAttachmentPositionsAreReplaced() throws Exception {
        for (String anchor : List.of("[I;0,0,0]", "[I;1,2]")) {
            var definition = actor("ITEM_FRAME", "{id:'minecraft:item_frame',block_pos:" + anchor + "}", 200.5, 90.5, 400.5);
            var repaired = TagStringIO.tagStringIO().asCompound(ReplayEntitySnapshots.prepare(definition, definition));
            assertArrayEquals(new int[]{200, 90, 400}, repaired.getIntArray("block_pos"));
        }
    }

    @Test void embeddedPassengersAreRemovedBeforeLoadingRatherThanAfterSpawningDuplicates() throws Exception {
        var definition = actor("BOAT", "{id:'minecraft:oak_boat',Passengers:[{id:'minecraft:item_frame'}],CustomName:'Recorded boat'}", 0, 90, 0);
        var repaired = TagStringIO.tagStringIO().asCompound(ReplayEntitySnapshots.prepare(definition, definition));
        assertFalse(repaired.keySet().contains("Passengers"));
        assertEquals("Recorded boat", repaired.getString("CustomName"));
        var ordinary = actor("ZOMBIE", "{id:'minecraft:zombie',IsBaby:1b}", 0, 90, 0);
        assertSame(ordinary.snapshot(), ReplayEntitySnapshots.prepare(ordinary, ordinary));
    }

    @Test void physicsBreaksAreCancelledOnlyInReplayWorlds() {
        for (boolean replay : new boolean[]{false, true}) {
            World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
                if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", replay ? "replay_session_world" : "arena_mobarmy_0");
                throw new AssertionError(method.getName());
            });
            Entity entity = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class}, (proxy, method, args) -> {
                if (method.getName().equals("getWorld")) return world;
                throw new AssertionError(method.getName());
            });
            var event = new EntityBreakEvent(entity, EntityBreakEvent.RemoveCause.PHYSICS);
            new ReplayListener().onEntityBreak(event);
            assertEquals(replay, event.isCancelled());
        }
    }

    private ReplayFrame.Actor actor(String type, String snapshot, double x, double y, double z) {
        return new ReplayFrame.Actor(UUID.randomUUID(), UUID.randomUUID(), type, "Actor", snapshot, null, null,
                x, y, z, 0, 0, "STANDING", false, false, null);
    }
}
