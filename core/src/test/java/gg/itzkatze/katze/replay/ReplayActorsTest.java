package gg.itzkatze.katze.replay;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayActorsTest {
    @Test
    void recordsVehiclesEvenWhenChunkEntitiesAreNotLoadedYet() {
        Entity boat = entity(Entity.class);
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> id;
            case "getVehicle" -> boat;
            default -> throw new AssertionError(method.getName());
        });
        var actors = ReplayActors.capture(List.of(player), List.of(chunk()), this::snapshot);
        assertEquals(List.of(id, boat.getUniqueId()), actors.stream().map(ReplayFrame.Actor::id).toList());
    }

    @Test
    void recordsParticipantsWhenTheirChunkEntityListsAreStillEmpty() {
        Player player = entity(Player.class);
        var actors = ReplayActors.capture(List.of(player), List.of(chunk()), this::snapshot);
        assertEquals(List.of(player.getUniqueId()), actors.stream().map(ReplayFrame.Actor::id).toList());
    }

    @Test
    void capturesEachParticipantOnceAndExcludesOtherPlayersFromChunks() {
        Player participant = entity(Player.class), other = entity(Player.class);
        LivingEntity mob = entity(LivingEntity.class);
        var actors = ReplayActors.capture(List.of(participant), List.of(chunk(participant, other, mob), chunk(mob)), this::snapshot);
        assertEquals(List.of(participant.getUniqueId(), mob.getUniqueId()), actors.stream().map(ReplayFrame.Actor::id).toList());
    }

    @Test void capturesEveryTeamsTrackedCombatMobsEvenWhenPlayersAreOnDistantRoofs() {
        Player firstTeam = entity(Player.class), secondTeam = entity(Player.class), spectator = entity(Player.class);
        LivingEntity firstMob = entity(LivingEntity.class), secondMob = entity(LivingEntity.class);
        var actors = ReplayActors.capture(List.of(firstTeam, secondTeam), List.of(chunk(firstMob)), List.of(firstMob, secondMob, spectator), this::snapshot);
        assertEquals(List.of(firstTeam.getUniqueId(), secondTeam.getUniqueId(), firstMob.getUniqueId(), secondMob.getUniqueId()),
                actors.stream().map(ReplayFrame.Actor::id).toList());
    }

    private ReplayFrame.Actor snapshot(Entity entity) {
        return new ReplayFrame.Actor(entity.getUniqueId(), UUID.randomUUID(), entity instanceof Player ? "PLAYER" : "ZOMBIE",
                "Actor", null, null, null, 0, 64, 0, 0, 0, "STANDING", false, false, List.of());
    }

    private Chunk chunk(Entity... entities) {
        return (Chunk) Proxy.newProxyInstance(Chunk.class.getClassLoader(), new Class<?>[]{Chunk.class}, (proxy, method, args) -> {
            if (method.getName().equals("getEntities")) return entities;
            throw new AssertionError(method.getName());
        });
    }

    private <T extends Entity> T entity(Class<T> type) {
        UUID id = UUID.randomUUID();
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("getUniqueId")) return id;
            if (method.getName().equals("getVehicle")) return null;
            throw new AssertionError(method.getName());
        }));
    }
}
