package gg.itzkatze.katze.replay;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayMountsTest {
    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
        if (method.getName().equals("equals")) return proxy == args[0];
        throw new AssertionError(method.getName());
    });
    private final Map<Entity, Entity> vehicles = new HashMap<>();
    private int mounts;

    @Test void ridersStayAttachedAndCanSwitchBetweenBoatsAndOtherRideablesThenDismount() {
        UUID player = UUID.randomUUID(), boat = UUID.randomUUID(), horse = UUID.randomUUID();
        Entity rider = entity(), boatEntity = entity(), horseEntity = entity();
        Map<UUID, Entity> actors = Map.of(player, rider, boat, boatEntity, horse, horseEntity);
        List<ReplayFrame.Actor> riding = List.of(actor(player, boat));
        ReplayMounts.attach(actors, riding);
        assertSame(boatEntity, vehicles.get(rider));
        ReplayMounts.detachChanged(actors, riding);
        ReplayMounts.attach(actors, riding);
        assertEquals(1, mounts);

        riding = List.of(actor(player, horse));
        ReplayMounts.detachChanged(actors, riding);
        assertNull(vehicles.get(rider));
        ReplayMounts.attach(actors, riding);
        assertSame(horseEntity, vehicles.get(rider));
        ReplayMounts.detachChanged(actors, List.of(actor(player, null)));
        assertNull(vehicles.get(rider));
        assertEquals(2, mounts);
    }

    private ReplayFrame.Actor actor(UUID id, UUID vehicle) {
        return new ReplayFrame.Actor(id, UUID.randomUUID(), "PLAYER", "Katze", null, null, null,
                0, 64, 0, 0, 0, "STANDING", false, false, List.of(), vehicle, false, 0, null, null);
    }

    private Entity entity() {
        return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getVehicle" -> vehicles.get(proxy);
            case "leaveVehicle" -> { vehicles.remove(proxy); yield true; }
            case "addPassenger" -> { vehicles.put((Entity) args[0], (Entity) proxy); mounts++; yield true; }
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError(method.getName());
        });
    }
}
