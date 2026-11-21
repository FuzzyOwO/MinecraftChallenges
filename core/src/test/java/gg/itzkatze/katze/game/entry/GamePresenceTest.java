package gg.itzkatze.katze.game.entry;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GamePresenceTest {
    @Test
    void staleGenerationCannotCompleteAdmission() {
        UUID player = UUID.randomUUID();
        GamePresence presence = new GamePresence(UUID.randomUUID());
        long generation = presence.generation(player);
        assertTrue(presence.transition(player, generation, GamePresence.State.PENDING));
        long pending = presence.generation(player);
        assertTrue(presence.transition(player, pending, GamePresence.State.LIMBO));
        assertFalse(presence.transition(player, pending, GamePresence.State.PLAYING));
        assertFalse(presence.isPlaying(player));
    }
}
