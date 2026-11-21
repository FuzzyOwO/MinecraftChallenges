package gg.itzkatze.katze.game;

import gg.itzkatze.katze.game.entry.EntryReason;
import gg.itzkatze.katze.game.entry.EntryResult;
import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameEntryRecoveryTest {
    @Test void oldSnapshotMigratesWithoutGrantingLateJoinEligibility() {
        UUID participant = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        SessionPersistence.restoreEntry(service, null, true, Set.of(participant));
        assertNotNull(service.currentRoundId());
        UUID lateJoiner = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return lateJoiner;
                    throw new AssertionError(method.getName());
                });
        assertEquals(EntryResult.DENIED, service.enter(player, service.currentRoundId(), EntryReason.REJOIN));
    }
    @Test void stoppedSessionDoesNotRestoreAStaleRound() {
        UUID participant = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        GameEntryService.Snapshot stale = new GameEntryService.Snapshot(1, UUID.randomUUID(), Set.of(participant), Map.of());
        SessionPersistence.restoreEntry(service, stale, false, Set.of(participant));
        assertNull(service.currentRoundId());
    }
    @Test void incompatibleEntrySnapshotIsNotMigratedAsLegacy() {
        GameEntryService.Snapshot incompatible = new GameEntryService.Snapshot(99, null, Set.of(), Map.of());
        assertThrows(IllegalStateException.class,
                () -> SessionPersistence.restoreEntry(new GameEntryService(), incompatible, true, Set.of(UUID.randomUUID())));
    }
}
