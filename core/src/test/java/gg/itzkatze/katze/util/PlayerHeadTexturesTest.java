package gg.itzkatze.katze.util;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerHeadTexturesTest {
    @Test
    void signedSkinsRemainAvailableOfflineAfterRestart(@TempDir Path directory) throws Exception {
        UUID player = UUID.randomUUID();
        var cache = new PlayerHeadTextures();
        cache.remember(player, PlayerHeadProfiles.readProfile("""
                {"id":"player","name":"Katze","properties":[{"name":"textures","value":"skin-data","signature":"signed-skin"}]}
                """));
        Path file = directory.resolve("player-skins.json");
        cache.save(file);
        var restarted = new PlayerHeadTextures();
        restarted.load(file);
        var offline = restarted.get(player);
        assertEquals(1, offline.size());
        assertEquals("textures", offline.getFirst().getName());
        assertEquals("skin-data", offline.getFirst().getValue());
        assertEquals("signed-skin", offline.getFirst().getSignature());
    }

    @Test
    void missingTexturesStayEmptyAndSuccessfulTexturesAreReused() {
        var cache = new PlayerHeadTextures();
        UUID player = UUID.randomUUID();
        assertTrue(cache.get(player).isEmpty());
        var texture = new ProfileProperty("textures", "skin", "signature");
        assertTrue(cache.remember(player, List.of(texture, new ProfileProperty("other", "value"))));
        assertEquals(List.of(texture), cache.get(player));
        assertTrue(cache.get(UUID.randomUUID()).isEmpty());
        var changed = new ProfileProperty("textures", "new-skin", "signature");
        assertTrue(cache.remember(player, List.of(changed)));
        assertFalse(cache.remember(player, List.of()));
        assertEquals(List.of(changed), cache.get(player));
    }
}
