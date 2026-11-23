package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MobLimitTable {

    static final MobLimitTable EMPTY = new MobLimitTable(Map.of());
    static final int UNLIMITED = Integer.MAX_VALUE;

    private static final Logger logger = LoggerFactory.getLogger("KatzeGames/MobLimitTable");
    private static final Pattern KEY_BEFORE_COLON = Pattern.compile("(?i)[A-Z][A-Z0-9_]*\\s*:");

    private final Map<EntityType, Integer> caps;

    private MobLimitTable(Map<EntityType, Integer> caps) {this.caps = caps;}

    static @Nullable MobLimitTable read(Path file) {
        if (!Files.exists(file)) {
            logger.warn("MobArmy mob limits are missing at {}; every mob type is uncapped", file);
            return EMPTY;
        }
        try {return parse(Files.readAllLines(file, StandardCharsets.UTF_8));}
        catch (IOException exception) {
            logger.warn("Unable to read MobArmy mob limits from {}: {}", file, exception.getMessage());
            return null;
        }
    }

    static MobLimitTable parse(List<String> lines) {
        Map<EntityType, Integer> caps = new HashMap<>();
        for (int row = 1; row <= lines.size(); row++) {
            String entry = lines.get(row - 1).replace("\uFEFF", "").split("#")[0].trim();
            if (entry.isEmpty()) continue;
            int separator = entry.indexOf(':');
            if (separator < 1) {
                logger.warn("mobarmy-limits.yml line {}: expected MOB_TYPE: cap, got {}", row, entry);
                continue;
            }
            String name = entry.substring(0, separator).trim();
            int cap;
            try {cap = Integer.parseInt(entry.substring(separator + 1).trim());}
            catch (NumberFormatException exception) {
                logger.warn("mobarmy-limits.yml line {}: {} is not a mob cap", row, entry);
                continue;
            }
            if (cap < 0) {
                logger.warn("mobarmy-limits.yml line {}: mob caps cannot be negative", row);
                continue;
            }
            try {caps.put(EntityType.valueOf(name.toUpperCase(Locale.ROOT)), cap);}
            catch (IllegalArgumentException exception) {
                logger.warn("mobarmy-limits.yml line {}: unknown mob type {}", row, name);
            }
        }
        return new MobLimitTable(caps);
    }

    static List<String> missingDefaultLines(List<String> defaults, List<String> installed) {
        Set<String> present = new HashSet<>();
        for (String line : installed) {
            Matcher found = KEY_BEFORE_COLON.matcher(line.replace("\uFEFF", ""));
            while (found.find()) present.add(stripColon(found.group()).toUpperCase(Locale.ROOT));
        }
        List<String> missing = new ArrayList<>();
        for (String line : defaults) {
            String entry = line.trim();
            if (entry.isEmpty() || entry.startsWith("#")) continue;
            Matcher key = KEY_BEFORE_COLON.matcher(entry);
            if (!key.find() || key.start() != 0) continue;
            if (!present.contains(stripColon(key.group()).toUpperCase(Locale.ROOT))) missing.add(entry);
        }
        return missing;
    }

    private static String stripColon(String matched) {return matched.substring(0, matched.length() - 1).trim();}

    int cap(EntityType type) {return caps.getOrDefault(type, UNLIMITED);}

    int size() {return caps.size();}

}
