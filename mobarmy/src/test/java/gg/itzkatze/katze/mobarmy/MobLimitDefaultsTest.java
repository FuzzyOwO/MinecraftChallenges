package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobLimitDefaultsTest {

    @Test void everyDefaultCapNamesAKnownMobType() throws IOException {
        List<String> lines;
        try (InputStream in = MobLimitDefaultsTest.class.getResourceAsStream("/data/mobarmy-limits.yml")) {
            assertNotNull(in, "data/mobarmy-limits.yml must ship inside the plugin");
            lines = new BufferedReader(new InputStreamReader(new BufferedInputStream(in), StandardCharsets.UTF_8)).lines().toList();
        }
        var limits = MobLimitTable.parse(lines);
        long listed = lines.stream().map(line -> line.split("#")[0].trim()).filter(entry -> !entry.isEmpty()).count();
        assertTrue(listed >= 80, "the default table should cover the whole mob list");
        assertEquals(listed, limits.size(), "every listed mob type must be a known EntityType");
    }
}
