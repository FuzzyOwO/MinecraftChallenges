package gg.itzkatze.katze.replay;

import com.google.gson.Gson;
import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayReturnStateTest {
    @Test void originalInventoryAndViewerFlagsSurviveRestartBeforeHotbarRestoration(@TempDir Path directory) throws Exception {
        var original = new ReplayService.ReturnState("katzegames:limbo", 4, 190, 8, 90, 0, "ADVENTURE", true, true,
                List.of("original-enchanted-item", "", "original-offhand"), 6, "original-cursor", false, false, true);
        Path file = directory.resolve("return.json");
        JsonFiles.write(file, original);
        assertEquals(original, JsonFiles.read(file, ReplayService.ReturnState.class));
    }

    @Test void olderReturnFilesRemainReadableWithoutReplacingThePlayersInventory() {
        var previous = new Gson().fromJson("""
                {"world":"katzegames:limbo","x":0,"y":190,"z":0,"yaw":0,"pitch":0,"mode":"ADVENTURE","flight":false,"flying":false}
                """, ReplayService.ReturnState.class);
        assertNull(previous.inventory());
        assertEquals("ADVENTURE", previous.mode());
    }
}
